package com.placewise.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.AppContext;
import com.placewise.exception.PlaceWiseException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

/**
 * Starts the real web server on a free port, with in-memory storage, and talks to it the way the browser
 * does. Every test begins with an empty application.
 */
@Timeout(120)
class WebServerTest {

    @TempDir
    Path tmp;

    private AppContext app;
    private WebServer server;
    private HttpClient client;
    private String base;

    private record Answer(int status, String contentType, String body) {
    }

    @BeforeEach
    void setUp() {
        app = AppContext.create(null, tmp.resolve("data"));
        server = new WebServer(app, 0, new PrintStream(OutputStream.nullOutputStream()));
        server.start();
        client = HttpClient.newHttpClient();
        base = "http://127.0.0.1:" + server.port();
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop();
        app.close();
    }

    private Answer get(String path) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(URI.create(base + path)).GET());
    }

    /** POSTs form fields the way the page does: name, value, name, value, ... */
    private Answer post(String path, String... namesAndValues) throws IOException, InterruptedException {
        return send(HttpRequest.newBuilder(URI.create(base + path)).header("X-PlaceWise", "1")
                .POST(HttpRequest.BodyPublishers.ofString(encode(namesAndValues))));
    }

    private Answer send(HttpRequest.Builder request) throws IOException, InterruptedException {
        HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        return new Answer(response.statusCode(), response.headers().firstValue("Content-Type").orElse(""),
                response.body());
    }

    private static String encode(String... namesAndValues) {
        StringBuilder form = new StringBuilder();
        for (int i = 0; i < namesAndValues.length; i += 2) {
            form.append(i == 0 ? "" : "&").append(namesAndValues[i]).append('=')
                    .append(URLEncoder.encode(namesAndValues[i + 1], StandardCharsets.UTF_8));
        }
        return form.toString();
    }

    private void loadSampleData() throws IOException, InterruptedException {
        assertThat(post("/api/import/sample", "sample", "students").body()).contains("Imported 50 of 50 rows");
        assertThat(post("/api/import/sample", "sample", "companies").body()).contains("Imported 5 of 5 rows");
    }

    // ----- the page itself -----

    @Test
    void thePageAndItsFilesAreServedFromTheJar() throws Exception {
        Answer page = get("/");
        assertThat(page.status()).isEqualTo(200);
        assertThat(page.contentType()).startsWith("text/html");
        assertThat(page.body()).contains("<title>PlaceWise</title>").contains("js/main.js");

        assertThat(get("/index.html").body()).isEqualTo(page.body());
        assertThat(get("/style.css").contentType()).startsWith("text/css");
        assertThat(get("/js/common.js").contentType()).startsWith("text/javascript");
        assertThat(get("/fonts/patrick-hand-sc.woff2").contentType()).isEqualTo("font/woff2");
    }

    @Test
    void nothingOutsideTheWebFolderCanBeRead() throws Exception {
        assertThat(get("/missing.js").status()).isEqualTo(404);
        assertThat(get("/../pom.xml").status()).isEqualTo(404);
        assertThat(get("/js/../../pom.xml").status()).isEqualTo(404);
        assertThat(get("/fonts/OFL-patrick-hand-sc.txt").status()).isEqualTo(404); // only html, css, js, woff2
    }

    // ----- the rules of the API -----

    @Test
    void anUnknownApiCallAnswers404AsJson() throws Exception {
        Answer answer = get("/api/nothing");
        assertThat(answer.status()).isEqualTo(404);
        assertThat(answer.contentType()).startsWith("application/json");
        assertThat(answer.body()).contains("No such API call: GET /api/nothing");
    }

    @Test
    void aPostWithoutThePageHeaderIsRefusedAndChangesNothing() throws Exception {
        Answer answer = send(HttpRequest.newBuilder(URI.create(base + "/api/import/sample"))
                .POST(HttpRequest.BodyPublishers.ofString("sample=students")));

        assertThat(answer.status()).isEqualTo(403);
        assertThat(get("/api/students").body()).contains("\"students\":[]");
    }

    @Test
    void aPortThatIsTakenFailsWithAReadableMessage() {
        PrintStream quiet = new PrintStream(OutputStream.nullOutputStream());
        assertThatThrownBy(() -> new WebServer(app, server.port(), quiet))
                .isInstanceOf(PlaceWiseException.class)
                .hasMessageContaining("Cannot start the web server on port " + server.port())
                .hasMessageContaining("--port");
    }

    // ----- students and companies -----

    @Test
    void anEmptyApplicationHasAnEmptyOverview() throws Exception {
        assertThat(get("/api/overview").body()).contains("\"storage\":\"in-memory (data is lost on exit)\"")
                .contains("\"totalStudents\":0").contains("\"branches\":[]");
    }

    @Test
    void aStudentCanBeAddedFoundUpdatedAndDeleted() throws Exception {
        Answer added = post("/api/students/add", "rollNo", "cu9001", "name", "Demo Student", "branch", "cse",
                "semester", "6", "cgpa", "7.49", "activeBacklogs", "1", "email", "");
        assertThat(added.status()).isEqualTo(200);
        assertThat(added.body()).isEqualTo("{\"message\":\"Added CU9001 - Demo Student.\"}");

        assertThat(get("/api/students?search=demo").body()).contains("\"rollNo\":\"CU9001\"")
                .contains("\"branch\":\"CSE\"").contains("\"cgpa\":7.49").contains("\"email\":null");

        Answer updated = post("/api/students/update", "rollNo", "CU9001", "name", "Demo Student", "branch", "IT",
                "semester", "7", "cgpa", "8", "activeBacklogs", "0", "email", "demo@example.edu");
        assertThat(updated.body()).contains("Updated CU9001.");
        assertThat(get("/api/students").body()).contains("\"branch\":\"IT\"").contains("\"cgpa\":8.00")
                .contains("\"email\":\"demo@example.edu\"");

        assertThat(post("/api/students/delete", "rollNo", "cu9001").body()).contains("Deleted CU9001 - Demo Student.");
        assertThat(get("/api/students").body()).doesNotContain("CU9001");
    }

    @Test
    void aBadStudentIsRefusedWithEveryProblemListed() throws Exception {
        Answer answer = post("/api/students/add", "rollNo", "cu9001", "name", "", "branch", "cse",
                "semester", "9", "cgpa", "11", "activeBacklogs", "1", "email", "not-an-email");

        assertThat(answer.status()).isEqualTo(400);
        assertThat(answer.body()).contains("\"problems\":[\"Name must not be blank\","
                + "\"Semester must be between 1 and 8\",\"CGPA must be between 0.00 and 10.00\","
                + "\"Email address is not valid\"]");
        assertThat(get("/api/students").body()).doesNotContain("CU9001");
    }

    @Test
    void lettersInANumberADuplicateAndAMissingStudentAreErrorsNotCrashes() throws Exception {
        Answer letters = post("/api/students/add", "rollNo", "cu9001", "name", "X", "branch", "cse",
                "semester", "abc", "cgpa", "7", "activeBacklogs", "0");
        assertThat(letters.status()).isEqualTo(400);
        assertThat(letters.body()).contains("'abc' is not a whole number (semester)");

        post("/api/students/add", "rollNo", "cu9001", "name", "X", "branch", "cse", "semester", "6", "cgpa", "7",
                "activeBacklogs", "0");
        Answer duplicate = post("/api/students/add", "rollNo", "CU9001", "name", "Y", "branch", "cse",
                "semester", "6", "cgpa", "7", "activeBacklogs", "0");
        assertThat(duplicate.status()).isEqualTo(400);
        assertThat(duplicate.body()).contains("CU9001");

        Answer missing = post("/api/students/delete", "rollNo", "nobody");
        assertThat(missing.status()).isEqualTo(400);
        assertThat(missing.body()).contains("No student with roll number NOBODY");
    }

    @Test
    void aCompanyCanBeAddedUpdatedAndDeleted() throws Exception {
        Answer added = post("/api/companies/add", "name", "Demo Corp", "minCgpa", "7.5", "maxBacklogs", "1",
                "branches", "cse, it", "packageLpa", "10", "seats", "2");
        assertThat(added.body()).contains("Added company 1 - Demo Corp.");
        assertThat(get("/api/companies").body()).contains("\"minCgpa\":7.50").contains("\"branches\":[\"CSE\",\"IT\"]")
                .contains("\"branchesLabel\":\"CSE, IT\"").contains("\"seats\":2");

        Answer updated = post("/api/companies/update", "id", "1", "name", "Demo Corp", "minCgpa", "8",
                "maxBacklogs", "0", "branches", "all", "packageLpa", "12.5", "seats", "3");
        assertThat(updated.body()).contains("Updated company 1 - Demo Corp.");
        assertThat(get("/api/companies").body()).contains("\"branches\":[]").contains("\"branchesLabel\":\"ALL\"")
                .contains("\"packageLpa\":12.50");

        Answer bad = post("/api/companies/add", "name", "", "minCgpa", "12", "maxBacklogs", "0", "branches", "",
                "packageLpa", "5", "seats", "0");
        assertThat(bad.status()).isEqualTo(400);
        assertThat(bad.body()).contains("Company name must not be blank").contains("Seats must be greater than zero");

        assertThat(post("/api/companies/delete", "id", "1").body()).contains("Deleted Demo Corp");
        assertThat(get("/api/companies").body()).isEqualTo("{\"companies\":[]}");
    }

    // ----- eligibility, ranking, insights -----

    @Test
    void eligibilityOfAStudentShowsWhichCriteriaFailAndTheExactGap() throws Exception {
        loadSampleData();

        String body = get("/api/eligibility/student?rollNo=cu2520").body(); // CSE, CGPA 7.85, 1 backlog

        assertThat(body).contains("\"name\":\"Tara Menon\"");
        assertThat(body).contains("\"name\":\"Acme Technologies\"").contains("\"eligible\":false,\"cgpaFails\":true,"
                + "\"backlogsFail\":true,\"branchFails\":false,\"remark\":\"Needs +0.15 CGPA and 1 fewer backlog\"");
        assertThat(body).contains("\"eligible\":true,\"cgpaFails\":false,\"backlogsFail\":false,\"branchFails\":false,"
                + "\"remark\":\"Eligible: meets every criterion\"");
        assertThat(body).contains("\"branchFails\":true,\"remark\":\"Branch CSE is not allowed (allowed: ECE, ME)\"");
        assertThat(Files.readString(tmp.resolve("data").resolve("audit.log")))
                .contains("ELIGIBILITY_CHECK | CU2520 vs Acme Technologies -> NOT ELIGIBLE");
    }

    @Test
    void eligibilityForACompanyChecksEveryStudent() throws Exception {
        loadSampleData();

        Answer answer = get("/api/eligibility/company?companyId=1");

        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.body()).contains("\"company\":{\"id\":1,\"name\":\"Acme Technologies\"")
                .contains("\"rollNo\":\"CU2501\"").contains("\"rollNo\":\"CU2550\"");
        assertThat(get("/api/eligibility/company?companyId=99").status()).isEqualTo(400);
    }

    @Test
    void theRankedListIsFilteredAndPaged() throws Exception {
        loadSampleData();

        String firstPage = get("/api/ranked?companyId=1&page=1&pageSize=3").body();
        assertThat(firstPage).contains("\"total\":11").contains("\"rollNo\":\"CU2501\"").contains("\"rank\":3")
                .doesNotContain("\"rank\":4");
        assertThat(get("/api/ranked?companyId=1&page=2&pageSize=3").body()).contains("\"rank\":4")
                .doesNotContain("\"rank\":3}");
        assertThat(get("/api/ranked?companyId=1&branch=it&minCgpa=8.5").body()).contains("\"total\":2")
                .contains("Vivaan Singh").contains("Ishita Kapoor").doesNotContain("Aarav Sharma");
        assertThat(get("/api/ranked?companyId=1&page=0").status()).isEqualTo(400);
    }

    @Test
    void whyNotAndWhatIfComeFromTheSameRules() throws Exception {
        loadSampleData();

        String why = get("/api/explain?rollNo=CU2520").body();
        assertThat(why).contains("\"companies\":5").contains("\"needs\":[\"+0.15 CGPA\",\"1 fewer backlog\"]")
                .contains("\"summary\":\"Needs 1 fewer backlog\"");

        String whatIf = get("/api/what-if?rollNo=CU2520&cgpaIncrease=0.15&backlogsCleared=1").body();
        assertThat(whatIf).contains("\"alreadyEligible\":[\"Bluepeak Systems\"]")
                .contains("\"newlyUnlocked\":[\"Acme Technologies\",\"Cedar Analytics\"]")
                .contains("\"stillBlocked\":[\"Delta Robotics\",\"Evergreen Infra\"]")
                .contains("\"cohortUnlocks\":{\"Acme Technologies\":");
        // the simulator judged a copy: the stored student is unchanged
        assertThat(get("/api/students?search=tara").body()).contains("\"cgpa\":7.85,\"activeBacklogs\":1");

        assertThat(get("/api/what-if?rollNo=CU2520&cgpaIncrease=-1").status()).isEqualTo(400);
    }

    // ----- the threads -----

    @Test
    void aGeneratedDriveHoldsEveryInvariantAndSavesNothing() throws Exception {
        Answer answer = post("/api/drive/generated", "students", "60", "companies", "3", "seats", "4",
                "recruiters", "4", "seed", "42");

        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.body()).contains("\"violations\":[]").contains("\"recruiters\":4")
                .contains("\"persistence\":\"Generated data: results are shown but not saved\"");
        assertThat(get("/api/overview").body()).contains("\"applications\":0");
        assertThat(post("/api/drive/generated", "recruiters", "99").body())
                .contains("recruiters must be between 1 and 64");
    }

    @Test
    void aDriveOnStoredDataNeedsDataAndThenSavesItsResults() throws Exception {
        Answer empty = post("/api/drive/stored", "recruiters", "4", "seed", "7");
        assertThat(empty.status()).isEqualTo(400);
        assertThat(empty.body()).contains("Add at least one student and one company first");

        loadSampleData();
        Answer answer = post("/api/drive/stored", "recruiters", "4", "seed", "7");

        assertThat(answer.body()).contains("\"violations\":[]").contains("Results saved");
        assertThat(get("/api/overview").body()).doesNotContain("\"applications\":0,");
        assertThat(get("/api/report?name=overall").body()).contains("[\"Total students\",\"50\"]");
    }

    @Test
    void theLockNeverOversellsAndTheLifecycleShowsSixStates() throws Exception {
        String race = post("/api/drive/race", "threads", "8", "seats", "2", "window", "2").body();
        assertThat(race).contains("\"totalSeats\":6").contains("\"safeGranted\":6,\"safeOversold\":0");

        String lifecycle = post("/api/drive/lifecycle").body();
        assertThat(lifecycle).containsSubsequence("\"NEW\"", "\"RUNNABLE\"", "\"BLOCKED\"", "\"WAITING\"",
                "\"TIMED_WAITING\"", "\"TERMINATED\"");
    }

    // ----- reports, exports, imports -----

    @Test
    void reportsAreShownAndExportedIntoTheDataFolder() throws Exception {
        loadSampleData();

        assertThat(get("/api/report?name=branch").body()).contains("\"title\":\"Branch-wise summary\"")
                .contains("[\"CSE\",\"17\"");
        assertThat(get("/api/report?name=ranked&companyId=2").body())
                .contains("Ranked eligible students - Bluepeak Systems").contains("[\"1\",\"CU2501\",\"Aarav Sharma\"");
        assertThat(get("/api/report?name=eligible&companyId=2").body()).contains("Eligible students - Bluepeak Systems");
        assertThat(get("/api/report?name=nonsense").status()).isEqualTo(400);

        assertThat(post("/api/export", "what", "reports").body()).contains("Wrote 24 files");
        assertThat(tmp.resolve("data").resolve("reports").resolve("overall-summary.csv")).exists();
        assertThat(post("/api/export", "what", "students").body()).contains("students-export.csv");
        assertThat(tmp.resolve("data").resolve("students-export.csv")).content().contains("CU2501");
        assertThat(post("/api/export", "what", "companies").body()).contains("companies-export.csv");
        assertThat(post("/api/export", "what", "audit").body()).contains("audit-backup.log");
        assertThat(post("/api/export", "what", "everything").status()).isEqualTo(400);
    }

    @Test
    void aFileWithMistakesImportsTheGoodRowsAndListsTheRest() throws Exception {
        Answer answer = post("/api/import/sample", "sample", "students-with-errors");

        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.body()).contains("\"dataRows\":13,\"imported\":3,\"rejected\":10")
                .contains("[\"3\",\"expected 7 columns but found 5\",\"CU2602,Wrong Column Count,CSE,6,8.10\"]")
                .contains("CGPA 'eight' is not a number");
        assertThat(tmp.resolve("data").resolve("rejects.csv")).exists();
        assertThat(post("/api/import/sample", "sample", "passwords").status()).isEqualTo(400);
    }

    @Test
    void anUploadedCsvIsImportedLikeAFile() throws Exception {
        String csv = "rollNo,name,branch,semester,cgpa,activeBacklogs,email\n"
                + "CU7001,\"Rao, Meena\",CSE,6,8.20,0,\n"
                + "CU7002,No Cgpa,CSE,6,,0,\n";

        Answer answer = post("/api/import/upload", "type", "students", "csv", csv);

        assertThat(answer.body()).contains("\"dataRows\":2,\"imported\":1,\"rejected\":1");
        assertThat(get("/api/students").body()).contains("\"name\":\"Rao, Meena\"").doesNotContain("CU7002");
        assertThat(post("/api/import/upload", "type", "passwords", "csv", csv).status()).isEqualTo(400);
    }
}
