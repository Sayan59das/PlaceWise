package com.placewise.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.placewise.AppContext;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.StudentSort;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

@Timeout(60)
class MainMenuTest {

    private static final Path SAMPLE_STUDENTS = Path.of("sample-data", "students.csv");
    private static final Path SAMPLE_COMPANIES = Path.of("sample-data", "companies.csv");

    @TempDir
    Path tmp;

    private AppContext app;

    @BeforeEach
    void setUp() {
        app = AppContext.create(null, tmp.resolve("data"));
    }

    @AfterEach
    void tearDown() {
        app.close();
    }

    private String run(String... lines) {
        ScriptedConsole console = new ScriptedConsole(lines);
        new MainMenu(console.io, app).run();
        return console.output();
    }

    private void loadSampleData() {
        app.imports().importStudents(SAMPLE_STUDENTS, tmp.resolve("rejects.csv"));
        app.imports().importCompanies(SAMPLE_COMPANIES, tmp.resolve("rejects.csv"));
    }

    private static int numberAfter(String text, String label) {
        Matcher matcher = Pattern.compile(Pattern.quote(label) + "\\s*(\\d+)").matcher(text);
        assertThat(matcher.find()).as("'%s' in the output", label).isTrue();
        return Integer.parseInt(matcher.group(1));
    }

    // ---- main loop --------------------------------------------------------------------------------

    @Test
    void zeroExitsCleanlyAfterShowingTheMenu() {
        String out = run("0");
        assertThat(out).contains("PlaceWise - Student Placement Eligibility Checker").contains(" 7. Placement drive")
                .contains(" 0. Exit").endsWith("Goodbye." + System.lineSeparator());
    }

    @Test
    void endOfInputIsAGracefulExitNotACrash() {
        assertThat(run()).contains("Input ended - shutting down.").contains("Goodbye.");
        assertThat(run("1")).contains("Input ended - shutting down."); // ends inside a submenu prompt
    }

    @Test
    void badMainMenuChoicesAreRepromptedUntilValid() {
        String out = run("abc", "", "42", "-1", "10", "0");
        assertThat(out).contains("'abc' is not a whole number.").contains("Please type a number.")
                .contains("Please enter a number in the range 0-9.").contains("Goodbye.");
    }

    @Test
    void anErrorInAScreenReturnsToTheMainMenuAndNeverStopsTheProgram() {
        String out = run(
                "3", "1", "NOPE",              // no companies yet -> error
                "5", "NOPE",                   // unknown student -> error
                "6", "NOPE",                   // unknown student -> error
                "4",                           // no companies yet -> error
                "7", "2", "1", "1",            // stored drive without data -> error
                "0");
        assertThat(out.split("Error: ", -1).length - 1).isEqualTo(5);
        assertThat(out).contains("There are no companies yet - add one first")
                .contains("No student with roll number NOPE").contains("Add at least one student and one company")
                .contains("Goodbye.");
    }

    // ---- students ---------------------------------------------------------------------------------

    @Test
    void addingAStudentSurvivesEveryKindOfBadInput() {
        String out = run("1", "1",
                "r1",                       // roll
                "", "Asha Rao",             // name: blank first
                "cse",                      // branch
                "abc", "9", "0", "6",       // semester: text, too big, too small, ok
                "x", "11", "-1", "8.555",   // CGPA: text, too big, negative, ok (rounds to 8.56)
                "-2", "100", "0",           // backlogs: negative, too big, ok
                "",                         // email: none
                "0", "0");
        assertThat(out).contains("This cannot be blank.").contains("Added R1 - Asha Rao.")
                .contains("'abc' is not a whole number.").contains("Please enter a value between 0.00 and 10.00.")
                .contains("'x' is not a number.");
        Student saved = app.students().get("R1");
        assertThat(saved.cgpa()).isEqualTo(new BigDecimal("8.56"));
        assertThat(saved.semester()).isEqualTo(6);
        assertThat(saved.branch()).isEqualTo("CSE");
        assertThat(saved.email()).isNull();
    }

    @Test
    void aDuplicateRollNumberShowsAnErrorAndReturnsToTheMainMenu() {
        String out = run("1", "1", "R1", "Asha", "CSE", "6", "8", "0", "",
                "1", "R1", "Other", "IT", "6", "7", "0", "",   // same roll again -> error, back to main menu
                "0");
        assertThat(out).contains("Error: Roll number already exists: R1");
        assertThat(app.students().get("R1").name()).isEqualTo("Asha");
    }

    @Test
    void updateKeepsEverythingYouLeaveBlank() {
        app.students().add(new Student("R1", "Asha", "CSE", 4, new BigDecimal("8.00"), 1, "a@b.co"));
        String out = run("1", "2", "r1", "", "", "", "9.1", "", "", "0", "0");
        assertThat(out).contains("Updated R1.");
        Student s = app.students().get("R1");
        assertThat(s.name()).isEqualTo("Asha");
        assertThat(s.semester()).isEqualTo(4);
        assertThat(s.cgpa()).isEqualTo(new BigDecimal("9.10"));
        assertThat(s.activeBacklogs()).isEqualTo(1);
        assertThat(s.email()).isEqualTo("a@b.co");
    }

    @Test
    void deleteAsksForConfirmation() {
        app.students().add(new Student("R1", "Asha", "CSE", 4, new BigDecimal("8.00"), 0, null));
        assertThat(run("1", "3", "R1", "n", "0", "0")).contains("Nothing deleted.");
        assertThat(app.students().find("R1")).isPresent();
        assertThat(run("1", "3", "R1", "y", "0", "0")).contains("Deleted.");
        assertThat(app.students().find("R1")).isEmpty();
        assertThat(run("1", "3", "GHOST", "0")).contains("Error: No student with roll number GHOST");
    }

    @Test
    void searchAndListWork() {
        loadSampleData();
        String out = run("1", "4", "cu2501", "5", "pillai", "5", "zzzz", "6", "cgpa", "6", "bogus", "6", "", "0", "0");
        assertThat(out).contains("Aarav Sharma").contains("Pillai, Meera").contains("No student matches.")
                .contains("Students by CGPA").contains("'bogus' is not a sort field; using ROLL_NO.");
    }

    // ---- companies --------------------------------------------------------------------------------

    @Test
    void companyScreenAddsListsUpdatesAndDeletes() {
        String out = run("2", "1", "Acme", "7.5", "1", "cse, it", "12.5", "3",
                "4",
                "2", "1", "Acme Ltd", "", "", "all", "", "",
                "4",
                "3", "1", "y",
                "4", "0", "0");
        assertThat(out).contains("Added company 1 - Acme.").contains("CSE, IT").contains("Updated company 1.")
                .contains("Acme Ltd").contains("Deleted.").contains("There are no companies yet.");
        assertThat(app.companies().listAll()).isEmpty();
    }

    @Test
    void updatingACompanyCanKeepReplaceOrClearBranches() {
        app.companies().add(new Company(0, "Acme", new BigDecimal("7"), 0, Set.of("CSE"), new BigDecimal("10"), 2));
        run("2", "2", "1", "", "", "", "", "", "", "0", "0");
        assertThat(app.companies().get(1).allowedBranches()).containsExactly("CSE");
        run("2", "2", "1", "", "", "", "ece it", "", "", "0", "0");
        assertThat(app.companies().get(1).allowedBranches()).containsExactly("ECE", "IT");
        run("2", "2", "1", "", "", "", "ALL", "", "", "0", "0");
        assertThat(app.companies().get(1).allowedBranches()).isEmpty();
    }

    // ---- eligibility, lists, insights -------------------------------------------------------------

    @Test
    void eligibilityScreenAnswersAllThreeQuestions() {
        loadSampleData();
        String out = run("3",
                "1", "CU2501", "1",   // Aarav vs Acme
                "1", "CU2550", "1",   // Mira vs Acme
                "2", "cu2511",        // one student against all companies
                "3", "3",             // all students against Cedar
                "0", "0");
        assertThat(out).contains("CU2501 (Aarav Sharma) vs Acme Technologies: ELIGIBLE")
                .contains("CU2550 (Mira Dutta) vs Acme Technologies: NOT ELIGIBLE - CGPA 5.80 is below required 8.00")
                .contains("4 active backlog(s) exceed the allowed maximum of 0")
                .contains("Eligible for ").contains(" of 5 companies.").contains(" of 50 students.");
    }

    @Test
    void eligibilityOfUnknownStudentOrCompanyIsAFriendlyError() {
        loadSampleData();
        assertThat(run("3", "1", "NOPE", "1", "0")).contains("Error: No student with roll number NOPE");
        assertThat(run("3", "1", "CU2501", "99", "0")).contains("Error: No company with id 99");
    }

    @Test
    void listsScreenShowsEligibleRankedTopNAndPages() {
        loadSampleData();
        String out = run("4", "3", "", "",              // Cedar Analytics, no filters
                "2",                                     // ranked
                "3", "3",                                // top 3
                "4", "5", "n", "n", "p", "q",            // pages of 5: next, next, previous, quit
                "0", "0");
        assertThat(out).contains("Ranked for Cedar Analytics").contains("Top 3 for Cedar Analytics")
                .contains("Page 1 for Cedar Analytics").contains("Page 2 for Cedar Analytics");
        assertThat(out.indexOf("Aarav Sharma")).isLessThan(out.indexOf("Ananya Gupta")); // 9.42 before 9.21
    }

    @Test
    void listsScreenFiltersByBranchAndMinimumCgpa() {
        loadSampleData();
        // Bluepeak (7.50, up to 1 backlog, CSE/IT/ECE); only ECE students with at least 7.8
        String out = run("4", "2", "ECE", "7.8", "1", "0", "0");
        assertThat(out).contains("Arjun Mehta").contains("Saanvi Reddy").contains("Navya Iyer")
                .doesNotContain("Aarav Sharma")      // CSE
                .doesNotContain("Zoya Qureshi")      // ECE but 7.50 < 7.8
                .doesNotContain("Vihaan Dubey");     // ECE but 7.70 < 7.8
        assertThat(run("4", "3", "MECH", "", "1", "2", "0", "0")).contains("No eligible students match.");
    }

    @Test
    void pagingPastTheEndGoesBackToTheLastPage() {
        loadSampleData();
        String out = run("4", "5", "", "", "4", "1000", "n", "q", "0", "0");
        assertThat(out).contains("That page is empty.");
    }

    @Test
    void explainShowsExactGapsForEveryRejectingCompany() {
        loadSampleData();
        String out = run("5", "CU2550");
        assertThat(out).contains("Why CU2550 (Mira Dutta, CGPA 5.80, 4 backlog(s), CE) is rejected:")
                .contains("Acme Technologies: Needs +2.20 CGPA and 4 fewer backlogs; Branch CE is not allowed")
                .contains("Cedar Analytics: Needs +1.20 CGPA and 4 fewer backlogs");
        assertThat(run("5", "NOPE", "0")).contains("Error: No student with roll number NOPE");
    }

    @Test
    void explainSaysSoWhenNobodyRejectsTheStudent() {
        app.students().add(new Student("R1", "Asha", "CSE", 6, new BigDecimal("9.00"), 0, null));
        app.companies().add(new Company(0, "Open", new BigDecimal("6"), 0, Set.of(), BigDecimal.TEN, 1));
        assertThat(run("5", "R1", "0")).contains("Not rejected anywhere: eligible for all 1 companies.");
    }

    @Test
    void whatIfShowsUnlockedCompaniesAndNeverChangesStoredData() {
        loadSampleData();
        Student before = app.students().get("CU2527"); // 7.50 CGPA, 1 backlog, CSE
        String out = run("6", "CU2527", "0.6", "1", "y");
        assertThat(out).contains("Scenario: CGPA 7.50 -> 8.10, backlogs 1 -> 0")
                .contains("Newly unlocked:").contains("Acme Technologies").contains("newly eligible student(s)");
        assertThat(app.students().get("CU2527")).isEqualTo(before);
    }

    @Test
    void whatIfWithNoImprovementUsesTheDefaults() {
        loadSampleData();
        String out = run("6", "CU2501", "", "", "n", "0");
        assertThat(out).contains("Scenario: CGPA 9.42 -> 9.42, backlogs 0 -> 0").contains("Newly unlocked:   (none)");
    }

    // ---- drive screen -----------------------------------------------------------------------------

    @Test
    void driveScreenRunsAGeneratedDriveAndReportsTheInvariants() {
        String out = run("7", "1", "60", "3", "4", "2", "5", "0", "0");
        assertThat(out).contains("Starting: 60 student thread(s), 2 recruiter thread(s)")
                .contains("Invariants: ALL HOLD").contains("Students placed:")
                .contains("Generated data: results are shown but not saved").doesNotContain("INVARIANT VIOLATIONS");
    }

    @Test
    void driveOnStoredDataNeedsDataThenSavesAndReplacesTheApplications() {
        assertThat(run("7", "2", "2", "7", "0")).contains("Error: Add at least one student and one company");
        loadSampleData();

        String first = run("7", "2", "2", "7", "0", "0");
        assertThat(first).contains("Invariants: ALL HOLD").contains("Results saved");
        int stored = numberAfter(run("8", "4", "0", "0", "0"), "Applications recorded");
        assertThat(stored).isPositive().isLessThanOrEqualTo(numberAfter(first, "Applications:"));

        String second = run("7", "2", "4", "8", "0", "0");
        int storedAgain = numberAfter(run("8", "4", "0", "0", "0"), "Applications recorded");
        // the table was REPLACED by the second drive: it holds at most that drive's applications
        assertThat(storedAgain).isPositive().isLessThanOrEqualTo(numberAfter(second, "Applications:"));
        assertThat(storedAgain).isLessThanOrEqualTo(50 * 5);
    }

    @Test
    void raceDemoPrintsBothOutcomesFromTheSameWorkload() {
        String out = run("7", "3", "8", "2", "5", "0", "0");
        assertThat(out).contains("UNSAFE (no synchronization):").contains("SAFE   (lock per company):")
                .contains("oversold 0");
    }

    @Test
    void lifecycleDemoPrintsAllSixThreadStates() {
        String out = run("7", "4", "0", "0");
        assertThat(out).contains("NEW ", "RUNNABLE", "BLOCKED", "WAITING", "TIMED_WAITING", "TERMINATED");
    }

    // ---- reports and import -----------------------------------------------------------------------

    @Test
    void reportScreenShowsAndExportsTextAndCsv() throws IOException {
        loadSampleData();
        Path dir = tmp.resolve("out");
        String out = run("8",
                "3", "1", dir.toString(),           // branch summary as text file
                "4", "2", dir.toString(),           // overall summary as CSV
                "1", "1", "0",                      // eligible list of company 1, just show it
                "2", "2", "2", dir.toString(),      // ranked list of company 2, as CSV
                "5", dir.toString(),                // every report
                "6", tmp.resolve("s.csv").toString(),
                "7", tmp.resolve("c.csv").toString(),
                "8",
                "0", "0");
        assertThat(out).contains("Branch-wise summary").contains("Overall placement summary")
                .contains("Wrote 24 files").contains("Wrote 50 students").contains("Wrote 5 companies")
                .contains("Audit log copied to");
        assertThat(dir.resolve("branch-wise-summary.txt")).exists();
        assertThat(dir.resolve("overall-placement-summary.csv")).exists();
        assertThat(dir.resolve("ranked-eligible-students-bluepeak-systems.csv")).exists();
        assertThat(dir.resolve("eligible-Acme_Technologies.csv")).exists();
        assertThat(Files.readAllLines(tmp.resolve("s.csv"))).hasSize(51);
        assertThat(Files.readAllLines(tmp.resolve("c.csv"))).hasSize(6);
        assertThat(tmp.resolve("data").resolve("audit-backup.log")).exists();
    }

    @Test
    void importScreenLoadsTheSampleFilesAndWritesRejectsForTheDirtyOne() throws IOException {
        Path rejects = tmp.resolve("rejects.csv");
        String out = run("9", "1", SAMPLE_STUDENTS.toString(), rejects.toString(),
                "2", SAMPLE_COMPANIES.toString(), rejects.toString(),
                "1", Path.of("sample-data", "students-with-errors.csv").toString(), rejects.toString(),
                "1", tmp.resolve("missing.csv").toString(), rejects.toString(),
                "0");
        assertThat(out).contains("Imported 50 of 50 rows; 0 rejected").contains("Imported 5 of 5 rows; 0 rejected")
                .contains("Imported 3 of 13 rows; 10 rejected").contains("Error: File not found");
        assertThat(app.students().listAll(com.placewise.ranking.StudentSort.ROLL_NO)).hasSize(53);
        List<String> lines = Files.readAllLines(rejects);
        assertThat(lines).hasSize(11); // header + 10 rejected rows, in file order
        assertThat(lines.get(1)).startsWith("3,expected 7 columns but found 5,");
        assertThat(lines).anyMatch(l -> l.contains("not closed")).anyMatch(l -> l.contains("out of range"))
                .anyMatch(l -> l.contains("duplicate of 'CU2601' already seen on line 2"));
    }
}
