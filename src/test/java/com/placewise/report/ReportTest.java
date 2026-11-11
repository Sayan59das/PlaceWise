package com.placewise.report;

import static com.placewise.Fixtures.company;
import static com.placewise.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;

import com.placewise.analytics.BranchSummary;
import com.placewise.analytics.StudentAnalytics;
import com.placewise.eligibility.EligibilityChecker;
import com.placewise.model.Application;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.StudentRanking;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportTest {

    @TempDir
    Path tmp;

    private final Company acme = company(1, "Acme", "7", 0, 2);
    private final List<Student> students = List.of(student("R2", "Bo", "IT", "8.00", 0),
            student("R1", "Asha, Jr", "CSE", "9.10", 0));

    @Test
    void textRendererAlignsColumnsAndCountsRows() {
        Table table = ReportBuilder.eligibleList(acme, students);
        assertThat(new TextReportRenderer().render(table)).isEqualTo("""
                Eligible students - Acme
                ========================

                Roll No  Name      Branch  Semester  CGPA  Backlogs
                -------  --------  ------  --------  ----  --------
                R1       Asha, Jr  CSE     6         9.10  0
                R2       Bo        IT      6         8.00  0

                2 rows
                """);
    }

    @Test
    void textRendererHandlesEmptyAndSingleRowTables() {
        String empty = new TextReportRenderer().render(new Table("Nothing", List.of("A", "B"), List.of()));
        assertThat(empty).contains("0 rows").contains("A  B");
        String one = new TextReportRenderer().render(new Table("One", List.of("A"), List.of(List.of("x"))));
        assertThat(one).contains("1 row\n").doesNotContain("1 rows");
    }

    @Test
    void csvRendererQuotesCommasAndOmitsTheTitle() {
        String csv = new CsvReportRenderer().render(ReportBuilder.eligibleList(acme, students));
        assertThat(csv).isEqualTo("""
                Roll No,Name,Branch,Semester,CGPA,Backlogs
                R1,"Asha, Jr",CSE,6,9.10,0
                R2,Bo,IT,6,8.00,0
                """);
    }

    @Test
    void rankedListHasRankColumnInRankingOrder() {
        Table table = ReportBuilder.rankedList(acme, StudentRanking.rank(students));
        assertThat(table.headers()).startsWith("Rank", "Roll No");
        assertThat(table.rows().get(0)).startsWith("1", "R1");
        assertThat(table.rows().get(1)).startsWith("2", "R2");
        assertThat(table.title()).isEqualTo("Ranked eligible students - Acme");
    }

    @Test
    void branchSummaryTableMatchesTheAnalytics() {
        List<BranchSummary> summaries = StudentAnalytics.branchSummaries(students);
        Table table = ReportBuilder.branchSummary(summaries);
        assertThat(table.rows()).containsExactly(List.of("CSE", "1", "9.10", "9.10"), List.of("IT", "1", "8.00", "8.00"));
    }

    @Test
    void overallSummaryTableListsEveryMetricInOrder() {
        var summary = StudentAnalytics.placementSummary(students, List.of(acme),
                List.of(new Application(students.get(1), acme, ApplicationStatus.SELECTED)),
                new com.placewise.eligibility.EligibilityChecker());
        Table table = ReportBuilder.overallSummary(summary);
        assertThat(table.headers()).containsExactly("Metric", "Value");
        assertThat(table.rows().get(0)).containsExactly("Total students", "2");
        assertThat(table.rows()).contains(List.of("Placement percentage", "50.00%"));
        assertThat(table.rows()).hasSize(13);
    }

    @Test
    void exporterWritesTheRenderedTextToTheNamedFile() throws IOException {
        Path file = ReportExporter.export(ReportBuilder.eligibleList(acme, students), new CsvReportRenderer(),
                tmp.resolve("out/deep"), "eligible-Acme");
        assertThat(file).isEqualTo(tmp.resolve("out/deep/eligible-Acme.csv"));
        assertThat(Files.readString(file)).startsWith("Roll No,Name");
    }

    @Test
    void exporterOverwritesAnExistingReport() throws IOException {
        Table table = new Table("T", List.of("A"), List.of(List.of("new")));
        ReportExporter.export(new Table("T", List.of("A"), List.of(List.of("old"))), new CsvReportRenderer(), tmp, "r");
        ReportExporter.export(table, new CsvReportRenderer(), tmp, "r");
        assertThat(Files.readString(tmp.resolve("r.csv"))).isEqualTo("A\nnew\n");
    }

    @Test
    void unsafeBaseNamesCannotEscapeTheReportDirectory() throws IOException {
        Table table = new Table("T", List.of("A"), List.of());
        Path evil = ReportExporter.export(table, new CsvReportRenderer(), tmp.resolve("reports"), "../../evil name");
        assertThat(evil.getParent()).isEqualTo(tmp.resolve("reports"));
        assertThat(evil.getFileName().toString()).isEqualTo(".._.._evil_name.csv");
        assertThat(ReportExporter.safeName("")).isEqualTo("report");
        assertThat(ReportExporter.safeName("..")).isEqualTo("report");
        assertThat(ReportExporter.safeName("a/b\\c:d")).isEqualTo("a_b_c_d");
    }

    @Test
    void formatsKnowTheirNamesAndExtensions() {
        assertThat(new TextReportRenderer().extension()).isEqualTo(".txt");
        assertThat(new TextReportRenderer().formatName()).isEqualTo("text");
        assertThat(new CsvReportRenderer().extension()).isEqualTo(".csv");
        assertThat(new CsvReportRenderer().formatName()).isEqualTo("csv");
        assertThat(BigDecimal.ZERO).isNotNull();
    }
}
