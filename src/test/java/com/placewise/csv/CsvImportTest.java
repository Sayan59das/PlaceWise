package com.placewise.csv;

import static com.placewise.Fixtures.company;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.exception.ImportException;
import com.placewise.exception.PlaceWiseException;
import com.placewise.model.Company;
import com.placewise.model.Student;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

class CsvImportTest {

    private static final String HEADER = "rollNo,name,branch,semester,cgpa,activeBacklogs,email";

    @TempDir
    Path tmp;

    private Path write(String name, String... lines) throws IOException {
        Path file = tmp.resolve(name);
        Files.writeString(file, String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
        return file;
    }

    private ImportResult<Student> read(Path file) {
        return StudentCsv.importer().read(file);
    }

    @Test
    void validFileIsFullyAccepted() throws IOException {
        Path file = write("ok.csv", HEADER, "R1,Asha Rao,cse,4,8.5,0,asha@uni.edu", "R2,Bo,IT,6,7.25,2,");
        ImportResult<Student> result = read(file);
        assertThat(result.rejected()).isEmpty();
        assertThat(result.dataRows()).isEqualTo(2);
        assertThat(result.items()).containsExactly(
                new Student("R1", "Asha Rao", "CSE", 4, new BigDecimal("8.50"), 0, "asha@uni.edu"),
                new Student("R2", "Bo", "IT", 6, new BigDecimal("7.25"), 2, null));
        assertThat(result.accepted().get(1).lineNumber()).isEqualTo(3);
    }

    @Test
    void missingFileIsAnImportExceptionNotACrash() {
        assertThatThrownBy(() -> read(tmp.resolve("nope.csv"))).isInstanceOf(ImportException.class)
                .hasMessageContaining("File not found");
    }

    @Test
    void aFolderIsNotAFile() {
        assertThatThrownBy(() -> read(tmp)).isInstanceOf(ImportException.class).hasMessageContaining("folder");
    }

    @Test
    void emptyFileIsReportedAsHavingNoHeader() throws IOException {
        Path file = tmp.resolve("empty.csv");
        Files.createFile(file);
        assertThatThrownBy(() -> read(file)).isInstanceOf(ImportException.class).hasMessageContaining("empty");
    }

    @Test
    void headerOnlyFileImportsNothingWithoutError() throws IOException {
        ImportResult<Student> result = read(write("header.csv", HEADER));
        assertThat(result.accepted()).isEmpty();
        assertThat(result.rejected()).isEmpty();
        assertThat(result.dataRows()).isZero();
    }

    @Test
    void wrongHeaderIsRejectedForTheWholeFile() throws IOException {
        Path file = write("bad.csv", "id,name", "1,x");
        assertThatThrownBy(() -> read(file)).isInstanceOf(ImportException.class)
                .hasMessageContaining("Unexpected header").hasMessageContaining(HEADER);
    }

    @Test
    void headerComparisonIgnoresCaseSpacesAndByteOrderMark() throws IOException {
        Path file = write("bom.csv", "﻿ ROLLNO , Name,BRANCH,semester,CGPA,activeBacklogs,EMAIL",
                "R1,Asha,CSE,4,8,0,");
        assertThat(read(file).accepted()).hasSize(1);
    }

    @Test
    void wrongColumnCountRejectsOnlyThatRow() throws IOException {
        ImportResult<Student> result = read(write("cols.csv", HEADER, "R1,Asha,CSE,4,8", "R2,Bo,IT,6,7,0,"));
        assertThat(result.items()).extracting(Student::rollNo).containsExactly("R2");
        assertThat(result.rejected()).hasSize(1);
        assertThat(result.rejected().get(0).lineNumber()).isEqualTo(2);
        assertThat(result.rejected().get(0).reason()).isEqualTo("expected 7 columns but found 5");
        assertThat(result.rejected().get(0).rawLine()).isEqualTo("R1,Asha,CSE,4,8");
    }

    @Test
    void nonNumericCgpaSemesterAndBacklogsAreRejectedWithClearReasons() throws IOException {
        ImportResult<Student> result = read(write("nan.csv", HEADER,
                "R1,A,CSE,4,abc,0,", "R2,B,CSE,x,8,0,", "R3,C,CSE,4,8,1.5,", "R4,D,CSE,4,NaN,0,"));
        assertThat(result.accepted()).isEmpty();
        assertThat(result.rejected()).extracting(RejectedRow::reason).containsExactly(
                "CGPA 'abc' is not a number", "semester 'x' is not a whole number",
                "activeBacklogs '1.5' is not a whole number", "CGPA 'NaN' is not a number");
    }

    @Test
    void cgpaAboveTenAndNegativeValuesAreRejectedByTheValidator() throws IOException {
        ImportResult<Student> result = read(write("range.csv", HEADER,
                "R1,A,CSE,4,10.01,0,", "R2,B,CSE,4,-1,0,", "R3,C,CSE,4,10.00,0,", "R4,D,CSE,4,0,-1,"));
        assertThat(result.items()).extracting(Student::rollNo).containsExactly("R3");
        assertThat(result.rejected()).hasSize(3);
        assertThat(result.rejected().get(0).reason()).contains("CGPA must be between 0.00 and 10.00");
        assertThat(result.rejected().get(2).reason()).contains("Active backlogs must not be negative");
    }

    @Test
    void absurdlyLargeExponentsAreRefusedQuicklyInsteadOfExhaustingMemory() throws IOException {
        ImportResult<Student> result = read(write("huge.csv", HEADER,
                "R1,A,CSE,4,1e999999999,0,", "R2,B,CSE,4,1e-999999999,0,", "R3,C,CSE,4,99999999999,0,"));
        assertThat(result.accepted()).isEmpty();
        assertThat(result.rejected()).hasSize(3).extracting(RejectedRow::reason).allMatch(r -> r.contains("out of range"));
    }

    @Test
    void aQuotedCommaStaysInsideTheName() throws IOException {
        ImportResult<Student> result = read(write("quote.csv", HEADER, "R1,\"Rao, Asha\",CSE,4,8,0,"));
        assertThat(result.items().get(0).name()).isEqualTo("Rao, Asha");
    }

    @Test
    void anUnclosedQuoteRejectsTheRowButNotTheImport() throws IOException {
        ImportResult<Student> result = read(write("unclosed.csv", HEADER, "R1,\"Rao,CSE,4,8,0,", "R2,Bo,IT,6,7,0,"));
        assertThat(result.items()).extracting(Student::rollNo).containsExactly("R2");
        assertThat(result.rejected().get(0).reason()).contains("not closed");
    }

    @Test
    void duplicateRollNumbersInTheFileRejectTheLaterRow() throws IOException {
        ImportResult<Student> result = read(write("dup.csv", HEADER, "R1,Asha,CSE,4,8,0,", "r1,Other,IT,6,9,0,"));
        assertThat(result.items()).extracting(Student::name).containsExactly("Asha");
        assertThat(result.rejected()).hasSize(1);
        assertThat(result.rejected().get(0).lineNumber()).isEqualTo(3);
        assertThat(result.rejected().get(0).reason()).isEqualTo("duplicate of 'R1' already seen on line 2");
    }

    @Test
    void blankLinesAreSkippedAndNotCounted() throws IOException {
        ImportResult<Student> result = read(write("blank.csv", HEADER, "", "R1,A,CSE,4,8,0,", "   ", "R2,B,CSE,4,8,0,"));
        assertThat(result.dataRows()).isEqualTo(2);
        assertThat(result.accepted()).hasSize(2);
    }

    @Test
    void windowsLineEndingsAreFine() throws IOException {
        Path file = tmp.resolve("crlf.csv");
        Files.writeString(file, HEADER + "\r\nR1,A,CSE,4,8,0,\r\nR2,B,CSE,4,8,0,\r\n");
        assertThat(read(file).accepted()).hasSize(2);
    }

    @Test
    @Timeout(30)
    void tenThousandRowsImportQuickly() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        Random random = new Random(3);
        for (int i = 0; i < 10_000; i++) {
            lines.add(String.format("R%05d,Student %d,CSE,%d,%d.%02d,%d,", i, i, 1 + random.nextInt(8),
                    5 + random.nextInt(5), random.nextInt(100), random.nextInt(3)));
        }
        Path file = tmp.resolve("big.csv");
        Files.write(file, lines, StandardCharsets.UTF_8);
        ImportResult<Student> result = read(file);
        assertThat(result.accepted()).hasSize(10_000);
        assertThat(result.rejected()).isEmpty();
    }

    @Test
    void exportThenReimportGivesIdenticalStudents() throws IOException {
        List<Student> original = List.of(
                new Student("R1", "Rao, Asha", "CSE", 4, new BigDecimal("8.50"), 0, "asha@uni.edu"),
                new Student("R2", "Quote \"Q\" Bo", "IT", 6, new BigDecimal("7.25"), 2, null),
                new Student("R3", "Plain", "ECE", 8, new BigDecimal("10.00"), 0, null),
                new Student("R4", "Zero", "ME", 1, new BigDecimal("0.00"), 7, "z@z.io"));
        Path file = tmp.resolve("export.csv");
        StudentCsv.export(file, original);
        ImportResult<Student> back = read(file);
        assertThat(back.rejected()).isEmpty();
        assertThat(back.items()).isEqualTo(original);
    }

    // ---- companies --------------------------------------------------------------------------------

    @Test
    void companyRowsParseBranchListsAndEmptyMeansAll() throws IOException {
        Path file = write("companies.csv", "name,minCgpa,maxBacklogs,allowedBranches,packageLpa,seats",
                "Acme,7.5,1,CSE|it,12.5,3", "Open,6,0,,4,10");
        ImportResult<Company> result = CompanyCsv.importer().read(file);
        assertThat(result.rejected()).isEmpty();
        assertThat(result.items().get(0).allowedBranches()).containsExactly("CSE", "IT");
        assertThat(result.items().get(0).minCgpa()).isEqualTo(new BigDecimal("7.50"));
        assertThat(result.items().get(1).allowedBranches()).isEmpty();
    }

    @Test
    void badCompanyRowsAreRejectedWithReasons() throws IOException {
        Path file = write("badcompanies.csv", "name,minCgpa,maxBacklogs,allowedBranches,packageLpa,seats",
                "A,11,0,,4,10", "B,7,0,,4,0", "C,7,x,,4,1", "D,7,0,,four,1", "E,7,0,,4", "F,7,0,,4,1", "f,8,0,,5,2");
        ImportResult<Company> result = CompanyCsv.importer().read(file);
        assertThat(result.items()).extracting(Company::name).containsExactly("F");
        assertThat(result.rejected()).extracting(RejectedRow::reason).containsExactly(
                "Invalid company: Minimum CGPA must be between 0.00 and 10.00",
                "Invalid company: Seats must be greater than zero",
                "maxBacklogs 'x' is not a whole number",
                "package 'four' is not a number",
                "expected 6 columns but found 5",
                "duplicate of 'f' already seen on line 7");
    }

    @Test
    void companyExportThenReimportKeepsEverythingExceptTheId() throws IOException {
        List<Company> original = List.of(company(1, "Acme, Inc", "7.50", 1, 3, "CSE", "IT"), company(2, "Open", "6.00", 0, 5));
        Path file = tmp.resolve("companies-out.csv");
        CompanyCsv.export(file, original);
        List<Company> back = CompanyCsv.importer().read(file).items();
        assertThat(back).hasSize(2);
        for (int i = 0; i < 2; i++) {
            assertThat(back.get(i)).isEqualTo(original.get(i).withId(0));
        }
    }

    // ---- rejects.csv ------------------------------------------------------------------------------

    @Test
    void rejectsFileIsValidCsvThatKeepsTheOriginalRow() throws IOException {
        Path rejects = tmp.resolve("rejects.csv");
        CsvFiles.writeRejects(rejects, List.of(new RejectedRow(7, "R1,\"Rao, A\",x", "bad, very bad")));
        List<String> lines = Files.readAllLines(rejects);
        assertThat(lines.get(0)).isEqualTo("line,reason,row");
        assertThat(CsvLine.parse(lines.get(1))).containsExactly("7", "bad, very bad", "R1,\"Rao, A\",x");
    }

    @Test
    void writingToAnUnwritableLocationFailsWithAReadableException() throws IOException {
        Path blocker = tmp.resolve("afile");
        Files.writeString(blocker, "x");
        assertThatThrownBy(() -> CsvFiles.write(blocker.resolve("sub").resolve("x.csv"), List.of("a"), List.of()))
                .isInstanceOf(com.placewise.exception.PlaceWiseException.class).hasMessageContaining("Cannot write");
    }
}
