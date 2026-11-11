package com.placewise.service;

import static com.placewise.Fixtures.company;
import static com.placewise.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.eligibility.EligibilityResult;
import com.placewise.exception.CompanyNotFoundException;
import com.placewise.exception.DuplicateRollNoException;
import com.placewise.exception.ImportException;
import com.placewise.exception.InvalidCompanyException;
import com.placewise.exception.InvalidStudentException;
import com.placewise.exception.PersistenceException;
import com.placewise.exception.StudentNotFoundException;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.RankedStudent;
import com.placewise.ranking.StudentSort;
import com.placewise.storage.StudentRepository;
import com.placewise.storage.memory.InMemoryStudentRepository;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ServicesTest {

    @TempDir
    Path tmp;

    private ServiceFixture app;

    @BeforeEach
    void setUp() {
        app = new ServiceFixture();
    }

    private void seed() {
        app.students.add(student("R1", "Asha", "CSE", "9.10", 0));
        app.students.add(student("R2", "Bo", "CSE", "8.00", 1));
        app.students.add(student("R3", "Cy", "IT", "8.60", 0));
        app.students.add(student("R4", "Di", "ECE", "6.50", 3));
        app.companies.add(company("Acme", "8.00", 1, 2, "CSE", "IT"));
        app.companies.add(company("Open", "6.00", 5, 5));
    }

    // ---- StudentService ---------------------------------------------------------------------------

    @Test
    void addValidatesAndRejectsDuplicates() {
        app.students.add(student("r1", "Asha", "CSE", "9", 0));
        assertThat(app.students.get(" R1 ").name()).isEqualTo("Asha");
        assertThatThrownBy(() -> app.students.add(student("R1", "Other", "IT", "5", 0)))
                .isInstanceOf(DuplicateRollNoException.class);
        assertThatThrownBy(() -> app.students.add(student("R9", "Bad", "CSE", "11", 0)))
                .isInstanceOf(InvalidStudentException.class);
        assertThat(app.students.find("R9")).isEmpty();
    }

    @Test
    void updateReplacesDataOrReportsMissingStudent() {
        app.students.add(student("R1", "Asha", "CSE", "9", 0));
        app.students.update(student("R1", "Asha Rao", "IT", "9.5", 1));
        assertThat(app.students.get("R1").name()).isEqualTo("Asha Rao");
        assertThatThrownBy(() -> app.students.update(student("R2", "X", "IT", "5", 0)))
                .isInstanceOf(StudentNotFoundException.class);
        assertThatThrownBy(() -> app.students.update(student("R1", "X", "IT", "55", 0)))
                .isInstanceOf(InvalidStudentException.class);
    }

    @Test
    void deleteAndGetReportMissingStudents() {
        app.students.add(student("R1", "Asha", "CSE", "9", 0));
        app.students.delete("r1");
        assertThat(app.students.find("R1")).isEmpty();
        assertThatThrownBy(() -> app.students.delete("R1")).isInstanceOf(StudentNotFoundException.class);
        assertThatThrownBy(() -> app.students.get("R1")).isInstanceOf(StudentNotFoundException.class)
                .hasMessageContaining("R1");
    }

    @Test
    void searchByNameFragmentIgnoresCaseAndSortsByName() {
        seed();
        app.students.add(student("R5", "Basanti", "CSE", "7", 0));
        assertThat(app.students.searchByName("  a ")).extracting(Student::name).containsExactly("Asha", "Basanti");
        assertThat(app.students.searchByName("ZZZ")).isEmpty();
        assertThat(app.students.searchByName("")).hasSize(5);
    }

    @Test
    void listAllUsesTheChosenSortField() {
        seed();
        assertThat(app.students.listAll(StudentSort.CGPA)).extracting(Student::rollNo)
                .containsExactly("R1", "R3", "R2", "R4");
        assertThat(app.students.listAll(StudentSort.BACKLOGS)).extracting(Student::rollNo)
                .containsExactly("R1", "R3", "R2", "R4");
        assertThat(app.students.listAll(StudentSort.BRANCH)).extracting(Student::branch)
                .containsExactly("CSE", "CSE", "ECE", "IT");
    }

    // ---- CompanyService ---------------------------------------------------------------------------

    @Test
    void companyCrudAndValidation() {
        Company saved = app.companies.add(company("Acme", "7.5", 1, 3, "CSE"));
        assertThat(saved.id()).isPositive();
        assertThat(app.companies.get(saved.id()).name()).isEqualTo("Acme");
        assertThat(app.companies.findByName("ACME")).contains(saved);
        app.companies.update(company(saved.id(), "Acme Ltd", "8", 0, 4));
        assertThat(app.companies.listAll()).extracting(Company::name).containsExactly("Acme Ltd");
        app.companies.delete(saved.id());
        assertThat(app.companies.listAll()).isEmpty();
    }

    @Test
    void companyErrorsAreSpecific() {
        assertThatThrownBy(() -> app.companies.add(company("", "11", 0, 0))).isInstanceOf(InvalidCompanyException.class);
        assertThatThrownBy(() -> app.companies.get(99)).isInstanceOf(CompanyNotFoundException.class);
        assertThatThrownBy(() -> app.companies.delete(99)).isInstanceOf(CompanyNotFoundException.class);
        assertThatThrownBy(() -> app.companies.update(company(99, "X", "7", 0, 1)))
                .isInstanceOf(CompanyNotFoundException.class);
        app.companies.add(company("Acme", "7", 0, 1));
        assertThatThrownBy(() -> app.companies.add(company("acme", "7", 0, 1)))
                .isInstanceOf(InvalidCompanyException.class);
    }

    // ---- EligibilityService -----------------------------------------------------------------------

    @Test
    void oneToOneAllToOneAndOneToAllAreCheckedAndAudited() {
        seed();
        long acme = app.companies.findByName("Acme").orElseThrow().id();
        assertThat(app.eligibility.check("R2", acme).eligible()).isTrue();     // 8.00 == min, 1 backlog == max
        assertThat(app.eligibility.check("R4", acme).eligible()).isFalse();
        assertThat(app.auditLines).hasSize(2).allMatch(l -> l.startsWith("ELIGIBILITY_CHECK: "));

        app.auditLines.clear();
        List<EligibilityResult> oneToAll = app.eligibility.checkAgainstAllCompanies("R4");
        assertThat(oneToAll).extracting(EligibilityResult::eligible).containsExactly(false, true);
        assertThat(app.auditLines).hasSize(2);

        app.auditLines.clear();
        List<EligibilityResult> allToOne = app.eligibility.checkAllStudents(acme);
        assertThat(allToOne).extracting(r -> r.student().rollNo()).containsExactly("R1", "R2", "R3", "R4");
        assertThat(allToOne).extracting(EligibilityResult::eligible).containsExactly(true, true, true, false);
        assertThat(app.auditLines).hasSize(4);
    }

    @Test
    void eligibilityOfUnknownThingsFailsWithTheRightException() {
        seed();
        assertThatThrownBy(() -> app.eligibility.check("NOPE", 1)).isInstanceOf(StudentNotFoundException.class);
        assertThatThrownBy(() -> app.eligibility.check("R1", 999)).isInstanceOf(CompanyNotFoundException.class);
        assertThatThrownBy(() -> app.eligibility.checkAgainstAllCompanies("NOPE")).isInstanceOf(StudentNotFoundException.class);
        assertThatThrownBy(() -> app.eligibility.checkAllStudents(999)).isInstanceOf(CompanyNotFoundException.class);
    }

    // ---- ListingService / EligibleFilter ----------------------------------------------------------

    @Test
    void eligibleListsHonourBranchAndMinCgpaFilters() {
        seed();
        long acme = app.companies.findByName("Acme").orElseThrow().id();
        assertThat(app.listings.eligibleStudents(acme, EligibleFilter.none())).extracting(Student::rollNo)
                .containsExactly("R1", "R2", "R3");
        assertThat(app.listings.eligibleStudents(acme, new EligibleFilter("cse", null))).extracting(Student::rollNo)
                .containsExactly("R1", "R2");
        assertThat(app.listings.eligibleStudents(acme, new EligibleFilter(null, new BigDecimal("8.60"))))
                .extracting(Student::rollNo).containsExactly("R1", "R3");
        assertThat(app.listings.eligibleStudents(acme, new EligibleFilter(" IT ", new BigDecimal("9")))).isEmpty();
        assertThat(new EligibleFilter("  ", null).branch()).isNull();
    }

    @Test
    void rankedTopAndPageAgreeWithTheRankingRules() {
        seed();
        long open = app.companies.findByName("Open").orElseThrow().id();
        List<RankedStudent> ranked = app.listings.ranked(open, EligibleFilter.none());
        assertThat(ranked).extracting(r -> r.student().rollNo()).containsExactly("R1", "R3", "R2", "R4");
        assertThat(app.listings.top(open, EligibleFilter.none(), 2)).extracting(r -> r.student().rollNo())
                .containsExactly("R1", "R3");
        assertThat(app.listings.page(open, EligibleFilter.none(), 2, 3)).extracting(RankedStudent::rank)
                .containsExactly(4);
        assertThatThrownBy(() -> app.listings.ranked(999, EligibleFilter.none())).isInstanceOf(CompanyNotFoundException.class);
    }

    // ---- ImportService ----------------------------------------------------------------------------

    private Path file(String name, String... lines) throws IOException {
        Path path = tmp.resolve(name);
        Files.writeString(path, String.join("\n", lines) + "\n");
        return path;
    }

    private static final String HEADER = "rollNo,name,branch,semester,cgpa,activeBacklogs,email";

    @Test
    void studentImportStoresGoodRowsAndWritesRejectsWithExactLineNumbers() throws IOException {
        app.students.add(student("R2", "Existing", "CSE", "7", 0));
        Path in = file("students.csv", HEADER, "R1,Asha,CSE,4,8.5,0,", "R2,Clash,IT,4,8,0,", "R3,Bad,CSE,4,abc,0,",
                "R4,Dee,IT,4,9,0,");
        Path rejects = tmp.resolve("rejects.csv");
        ImportSummary summary = app.imports.importStudents(in, rejects);
        assertThat(summary.dataRows()).isEqualTo(4);
        assertThat(summary.imported()).isEqualTo(2);
        assertThat(summary.rejected()).isEqualTo(2);
        assertThat(summary.rejectsFile()).isEqualTo(rejects);
        assertThat(summary.describe()).contains("Imported 2 of 4 rows; 2 rejected").contains("rejects.csv");
        assertThat(app.students.listAll(StudentSort.ROLL_NO)).extracting(Student::rollNo).containsExactly("R1", "R2", "R4");
        assertThat(app.students.get("R2").name()).isEqualTo("Existing"); // the clash did not overwrite
        List<String> lines = Files.readAllLines(rejects);
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1)).startsWith("3,roll number R2 already exists,");
        assertThat(lines.get(2)).startsWith("4,CGPA 'abc' is not a number,");
    }

    @Test
    void aCleanImportWritesNoRejectsFile() throws IOException {
        Path rejects = tmp.resolve("rejects.csv");
        ImportSummary summary = app.imports.importStudents(file("ok.csv", HEADER, "R1,Asha,CSE,4,8.5,0,"), rejects);
        assertThat(summary.rejectsFile()).isNull();
        assertThat(summary.describe()).isEqualTo("Imported 1 of 1 rows; 0 rejected");
        assertThat(rejects).doesNotExist();
    }

    @Test
    void anUnusableFileIsAnImportException() {
        assertThatThrownBy(() -> app.imports.importStudents(tmp.resolve("missing.csv"), tmp.resolve("r.csv")))
                .isInstanceOf(ImportException.class);
        assertThatThrownBy(() -> app.imports.importCompanies(tmp.resolve("missing.csv"), tmp.resolve("r.csv")))
                .isInstanceOf(ImportException.class);
    }

    @Test
    void ifTheStorageRefusesTheBatchNothingIsImportedAndTheUserIsTold() throws IOException {
        StudentRepository failing = new StudentRepository() {
            private final InMemoryStudentRepository delegate = new InMemoryStudentRepository();

            @Override
            public void saveAll(List<Student> students) {
                throw new PersistenceException("disk on fire"); // the one method under test fails
            }

            @Override
            public Student save(Student entity) {
                return delegate.save(entity);
            }

            @Override
            public Optional<Student> findById(String id) {
                return delegate.findById(id);
            }

            @Override
            public List<Student> findAll() {
                return delegate.findAll();
            }

            @Override
            public boolean update(Student entity) {
                return delegate.update(entity);
            }

            @Override
            public boolean delete(String id) {
                return delegate.delete(id);
            }
        };
        ImportService service = new ImportService(failing, app.companyRepo);
        assertThatThrownBy(() -> service.importStudents(file("s.csv", HEADER, "R1,Asha,CSE,4,8.5,0,"), tmp.resolve("r.csv")))
                .isInstanceOf(ImportException.class).hasMessageContaining("Nothing was imported")
                .hasMessageContaining("disk on fire");
        assertThat(failing.count()).isZero();
    }

    @Test
    void companyImportRejectsNameClashesRowByRow() throws IOException {
        app.companies.add(company("Acme", "7", 0, 1));
        Path in = file("c.csv", "name,minCgpa,maxBacklogs,allowedBranches,packageLpa,seats", "ACME,7,0,,5,2",
                "Beta,8,1,CSE|IT,9,3", "Gamma,7,0,,5,x");
        ImportSummary summary = app.imports.importCompanies(in, tmp.resolve("rc.csv"));
        assertThat(summary.imported()).isEqualTo(1);
        assertThat(summary.rejected()).isEqualTo(2);
        assertThat(app.companies.listAll()).extracting(Company::name).containsExactly("Acme", "Beta");
        List<String> lines = Files.readAllLines(tmp.resolve("rc.csv"));
        assertThat(lines.get(1)).startsWith("2,Invalid company: Company name already exists: ACME,");
        assertThat(lines.get(2)).startsWith("4,seats 'x' is not a whole number,");
    }

    // ---- ReportService ----------------------------------------------------------------------------

    @Test
    void exportAllWritesEveryReportInBothFormats() throws IOException {
        seed();
        List<Path> files = app.reports.exportAll(tmp.resolve("reports"));
        assertThat(files).hasSize(12); // (2 companies x 2 lists + branch + overall) x 2 formats
        assertThat(files).allSatisfy(f -> assertThat(f).isNotEmptyFile());
        assertThat(files).extracting(p -> p.getFileName().toString()).contains("eligible-Acme.txt",
                "ranked-Acme.csv", "branch-summary.txt", "overall-summary.csv", "ranked-Open.txt");
        String rankedAcme = Files.readString(tmp.resolve("reports/ranked-Acme.csv"));
        assertThat(rankedAcme).isEqualTo("""
                Rank,Roll No,Name,Branch,Semester,CGPA,Backlogs
                1,R1,Asha,CSE,6,9.10,0
                2,R3,Cy,IT,6,8.60,0
                3,R2,Bo,CSE,6,8.00,1
                """);
        assertThat(Files.readString(tmp.resolve("reports/branch-summary.csv"))).isEqualTo("""
                Branch,Students,Average CGPA,Highest CGPA
                CSE,2,8.55,9.10
                ECE,1,6.50,6.50
                IT,1,8.60,8.60
                """);
    }

    @Test
    void exportStudentsAndCompaniesWriteReimportableFiles() throws IOException {
        seed();
        assertThat(app.reports.exportStudents(tmp.resolve("s.csv"))).isEqualTo(4);
        assertThat(app.reports.exportCompanies(tmp.resolve("c.csv"))).isEqualTo(2);
        ServiceFixture fresh = new ServiceFixture();
        assertThat(fresh.imports.importStudents(tmp.resolve("s.csv"), tmp.resolve("r.csv")).imported()).isEqualTo(4);
        assertThat(fresh.imports.importCompanies(tmp.resolve("c.csv"), tmp.resolve("r2.csv")).imported()).isEqualTo(2);
        assertThat(fresh.students.listAll(StudentSort.ROLL_NO)).isEqualTo(app.students.listAll(StudentSort.ROLL_NO));
    }
}
