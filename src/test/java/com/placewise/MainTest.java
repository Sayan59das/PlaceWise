package com.placewise;

import static org.assertj.core.api.Assertions.assertThat;

import com.placewise.storage.database.TestDb;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Boots the real {@link Main}. Every test passes {@code --no-db} or {@code --db}, so none can ever read a
 * developer's real db.properties from the working directory.
 */
@Timeout(120)
class MainTest {

    @TempDir
    Path tmp;

    private record Outcome(int exitCode, String output) {
    }

    private Outcome run(String input, String... args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        int code = Main.run(args, new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(bytes, true, StandardCharsets.UTF_8));
        return new Outcome(code, bytes.toString(StandardCharsets.UTF_8));
    }

    private Path propertiesFor(String url) throws IOException {
        Path file = tmp.resolve("db.properties");
        Files.writeString(file, "db.url=" + url + "\ndb.user=sa\ndb.password=\n");
        return file;
    }

    private static int count(String url, String table) throws SQLException {
        try (Connection c = DriverManager.getConnection(url, "sa", "");
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    @Test
    void helpPrintsUsageAndSucceeds() {
        Outcome outcome = run("", "--help");
        assertThat(outcome.exitCode()).isZero();
        assertThat(outcome.output()).contains("Usage: java -jar placewise.jar").contains("--no-db");
    }

    @ParameterizedTest
    @ValueSource(strings = {"--bogus", "--db", "--data-dir", "--port"})
    void badArgumentsPrintUsageAndReturnTwo(String argument) {
        Outcome outcome = run("", argument);
        assertThat(outcome.exitCode()).isEqualTo(2);
        assertThat(outcome.output()).contains("Usage:");
    }

    @Test
    void dbAndNoDbTogetherAreRefused() {
        Outcome outcome = run("", "--db", "x.properties", "--no-db");
        assertThat(outcome.exitCode()).isEqualTo(2);
        assertThat(outcome.output()).contains("either --db or --no-db");
    }

    @Test
    void aMissingDbFileFailsClearlyWithATip() {
        Outcome outcome = run("0\n", "--db", tmp.resolve("absent.properties").toString(), "--data-dir",
                tmp.resolve("d").toString());
        assertThat(outcome.exitCode()).isEqualTo(2);
        assertThat(outcome.output()).contains("Cannot continue: Cannot read database settings")
                .contains("run with --no-db");
    }

    @Test
    void anUnreachableDatabaseFailsAtStartupNotMidMenu() throws IOException {
        Path props = propertiesFor("jdbc:h2:tcp://localhost:1/nothing");
        Outcome outcome = run("0\n", "--db", props.toString(), "--data-dir", tmp.resolve("d").toString());
        assertThat(outcome.exitCode()).isEqualTo(2);
        assertThat(outcome.output()).contains("Cannot connect to the database");
    }

    @Test
    void noDbModeRunsAndShutsDownGracefullyWithAuditBackup() throws IOException {
        Path data = tmp.resolve("data");
        Outcome outcome = run("0\n", "--no-db", "--data-dir", data.toString());
        assertThat(outcome.exitCode()).isZero();
        assertThat(outcome.output()).contains("storage: in-memory").contains("Goodbye.")
                .contains("Audit log backed up to");
        assertThat(data.resolve("audit.log")).exists();
        assertThat(data.resolve("audit-backup.log")).exists();
        assertThat(Files.readAllBytes(data.resolve("audit-backup.log")))
                .isEqualTo(Files.readAllBytes(data.resolve("audit.log")));
    }

    @Test
    void webModeStartsTheBrowserVersionAndStopsCleanlyWhenTheInputEnds() {
        Path data = tmp.resolve("data");
        Outcome outcome = run("\n", "--no-db", "--web", "--port", "0", "--data-dir", data.toString());

        assertThat(outcome.exitCode()).isZero();
        assertThat(outcome.output()).contains("PlaceWise web version: open http://localhost:")
                .contains("Web server stopped.").contains("Audit log backed up to")
                .doesNotContain("===== PlaceWise"); // the terminal menu is not shown in web mode
        assertThat(data.resolve("audit-backup.log")).exists();
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "-1", "70000"})
    void aBadPortPrintsUsageAndReturnsTwo(String port) {
        Outcome outcome = run("", "--no-db", "--web", "--port", port);
        assertThat(outcome.exitCode()).isEqualTo(2);
        assertThat(outcome.output()).contains("--port needs a number between 0 and 65535").contains("--web");
    }

    @Test
    void endOfInputInNoDbModeStillExitsZero() {
        Outcome outcome = run("", "--no-db", "--data-dir", tmp.resolve("data").toString());
        assertThat(outcome.exitCode()).isZero();
        assertThat(outcome.output()).contains("Input ended - shutting down.");
    }

    @Test
    void endToEndAgainstH2ImportDriveReportAndPersistenceAcrossRestarts() throws Exception {
        String url = TestDb.fresh().url();
        Path props = propertiesFor(url);
        Path data = tmp.resolve("data");
        String students = Path.of("sample-data", "students.csv").toString();
        String companies = Path.of("sample-data", "companies.csv").toString();
        String rejects = tmp.resolve("rejects.csv").toString();
        String script = String.join("\n",
                "9", "1", students, rejects, "2", companies, rejects, "0",            // import both sample files
                "3", "3", "1", "0",                                                     // all students vs company 1
                "7", "2", "3", "11", "0",                                               // drive on stored data
                "8", "4", "0", "0",                                                     // overall summary on screen
                "0") + "\n";
        Outcome first = run(script, "--db", props.toString(), "--data-dir", data.toString());

        assertThat(first.exitCode()).isZero();
        assertThat(first.output()).contains("Imported 50 of 50 rows; 0 rejected")
                .contains("Imported 5 of 5 rows; 0 rejected").contains("Invariants: ALL HOLD")
                .contains("Results saved").contains("Students placed:").contains("Placement percentage")
                .contains("Goodbye.");
        assertThat(count(url, "students")).isEqualTo(50);
        assertThat(count(url, "companies")).isEqualTo(5);
        assertThat(count(url, "applications")).isPositive();
        assertThat(count(url, "company_branches")).isEqualTo(9); // 2 + 3 + 0 + 2 + 2

        List<String> auditLines = Files.readAllLines(data.resolve("audit.log"));
        assertThat(auditLines).anyMatch(l -> l.contains("| ELIGIBILITY_CHECK |"))
                .anyMatch(l -> l.contains("| APPLICATION |")).anyMatch(l -> l.contains("| SELECTED |"));

        // restart against the same database: the data must still be there
        Outcome second = run("1\n6\n\n0\n0\n", "--db", props.toString(), "--data-dir", data.toString());
        assertThat(second.exitCode()).isZero();
        assertThat(second.output()).contains("CU2501").contains("CU2550");
    }

}
