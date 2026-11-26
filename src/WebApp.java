import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;

public class WebApp {
    private static PlacementManager manager = new PlacementManager();

    public static void main(String[] args) {
        try {
            manager.addCompany(new Company("TCS", 6.0, 1));
            manager.addCompany(new Company("Infosys", 7.0, 0));
            manager.addCompany(new Company("Amazon", 8.5, 0));
            manager.addCompany(new Company("Wipro", 6.5, 2));
            int count;
            try {
                count = manager.loadStudentsFromFile("data/students.csv");
            } catch (IOException e) {
                System.out.println("Could not read data/students.csv");
                System.out.println("Please run the program from the project folder (JAVA_proj).");
                return;
            }

            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/", WebApp::home);
            server.createContext("/check", WebApp::check);
            server.createContext("/addStudent", WebApp::addStudent);
            server.createContext("/addCompany", WebApp::addCompany);
            server.createContext("/export", WebApp::export);
            server.createContext("/style.css", WebApp::style);
            server.start();

            System.out.println("Loaded " + count + " students from data/students.csv");
            System.out.println("Open http://localhost:8080 in your browser");
            System.out.println("Press Ctrl+C to stop the server");
        } catch (Exception e) {
            System.out.println("Could not start the web app: " + e.getMessage());
        }
    }

    private static void home(HttpExchange ex) throws IOException {
        String page;
        try {
            page = readPage("index.html");
        } catch (IOException e) {
            sendText(ex, "web/index.html was not found. Run the program from the project folder.");
            return;
        }

        HashMap<String, String> query = parseForm(ex.getRequestURI().getRawQuery());
        String message = "";
        String msg = query.get("msg");
        if (msg != null) {
            message = "<p class=\"msg\">" + escape(msg) + "</p>";
        }

        ArrayList<Student> students = manager.getAllStudents();
        ArrayList<Company> companies = manager.getAllCompanies();

        StringBuilder studentRows = new StringBuilder();
        for (Student s : students) {
            studentRows.append("<tr><td>" + escape(s.getRollNo()) + "</td><td>" + escape(s.getName())
                    + "</td><td>" + escape(s.getBranch()) + "</td><td>" + s.getCgpa()
                    + "</td><td>" + s.getBacklogs() + "</td></tr>\n");
        }
        if (students.isEmpty()) {
            studentRows.append("<tr><td colspan=\"5\">No students yet.</td></tr>\n");
        }

        StringBuilder companyRows = new StringBuilder();
        for (Company c : companies) {
            companyRows.append("<tr><td>" + escape(c.getName()) + "</td><td>" + c.getMinCgpa()
                    + "</td><td>" + c.getMaxBacklogs() + "</td><td>" + countEligible(c) + "</td></tr>\n");
        }
        if (companies.isEmpty()) {
            companyRows.append("<tr><td colspan=\"4\">No companies yet.</td></tr>\n");
        }

        StringBuilder studentOptions = new StringBuilder();
        for (Student s : students) {
            studentOptions.append("<option value=\"" + escape(s.getRollNo()) + "\">"
                    + escape(s.getRollNo()) + " - " + escape(s.getName()) + "</option>\n");
        }

        StringBuilder companyOptions = new StringBuilder();
        for (Company c : companies) {
            companyOptions.append("<option value=\"" + escape(c.getName()) + "\">"
                    + escape(c.getName()) + "</option>\n");
        }

        StringBuilder rankRows = new StringBuilder();
        int rank = 1;
        for (Student s : manager.getRankedStudents()) {
            rankRows.append("<tr><td>" + rank + "</td><td>" + escape(s.getRollNo()) + "</td><td>"
                    + escape(s.getName()) + "</td><td>" + s.getCgpa() + "</td><td>"
                    + s.getBacklogs() + "</td></tr>\n");
            rank++;
        }

        StringBuilder eligibleBlocks = new StringBuilder();
        for (Company c : companies) {
            eligibleBlocks.append("<h3>" + escape(c.getName()) + " (Minimum CGPA: " + c.getMinCgpa()
                    + ", Maximum Backlogs: " + c.getMaxBacklogs() + ")</h3>\n");
            ArrayList<Student> eligible = getEligible(c.getName());
            if (eligible.isEmpty()) {
                eligibleBlocks.append("<p>No eligible students.</p>\n");
            } else {
                eligibleBlocks.append("<ul>\n");
                for (Student s : eligible) {
                    eligibleBlocks.append("<li>" + escape(s.getRollNo()) + " - " + escape(s.getName())
                            + " - CGPA " + s.getCgpa() + "</li>\n");
                }
                eligibleBlocks.append("</ul>\n");
            }
        }

        page = page.replace("{{MESSAGE}}", message);
        page = page.replace("{{STUDENT_ROWS}}", studentRows.toString());
        page = page.replace("{{STUDENT_COUNT}}", String.valueOf(students.size()));
        page = page.replace("{{COMPANY_ROWS}}", companyRows.toString());
        page = page.replace("{{STUDENT_OPTIONS}}", studentOptions.toString());
        page = page.replace("{{COMPANY_OPTIONS}}", companyOptions.toString());
        page = page.replace("{{RANK_ROWS}}", rankRows.toString());
        page = page.replace("{{ELIGIBLE_BLOCKS}}", eligibleBlocks.toString());

        sendHtml(ex, page);
    }

    private static void check(HttpExchange ex) throws IOException {
        String page;
        try {
            page = readPage("result.html");
        } catch (IOException e) {
            sendText(ex, "web/result.html was not found. Run the program from the project folder.");
            return;
        }

        HashMap<String, String> query = parseForm(ex.getRequestURI().getRawQuery());
        String roll = text(query, "roll");
        String companyName = text(query, "company");

        String style = "no";
        String result;
        try {
            if (manager.checkEligibility(roll, companyName)) {
                style = "ok";
                result = "Student " + roll + " is ELIGIBLE for " + companyName + ".";
            } else {
                result = "Student " + roll + " is NOT ELIGIBLE for " + companyName + ".";
            }
        } catch (InvalidDataException e) {
            result = e.getMessage();
        }

        page = page.replace("{{CLASS}}", style);
        page = page.replace("{{RESULT}}", escape(result));
        sendHtml(ex, page);
    }

    private static void addStudent(HttpExchange ex) throws IOException {
        HashMap<String, String> form = parseForm(readBody(ex));
        String message;
        String roll = text(form, "roll");
        if (manager.findStudent(roll) != null) {
            message = "Roll number " + roll + " already exists.";
        } else {
            try {
                Student s = new Student(roll, text(form, "name"), text(form, "branch"),
                        Double.parseDouble(text(form, "cgpa")),
                        Integer.parseInt(text(form, "backlogs")));
                manager.addStudent(s);
                message = "Student " + roll + " added.";
            } catch (InvalidDataException e) {
                message = "Could not add student: " + e.getMessage();
            } catch (NumberFormatException e) {
                message = "Could not add student: CGPA and backlogs must be numbers.";
            }
        }
        redirect(ex, "/?msg=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }

    private static void addCompany(HttpExchange ex) throws IOException {
        HashMap<String, String> form = parseForm(readBody(ex));
        String message;
        String name = text(form, "name");
        if (manager.findCompany(name) != null) {
            message = "Company " + name + " already exists.";
        } else {
            try {
                Company c = new Company(name, Double.parseDouble(text(form, "minCgpa")),
                        Integer.parseInt(text(form, "maxBacklogs")));
                manager.addCompany(c);
                message = "Company " + name + " added.";
            } catch (InvalidDataException e) {
                message = "Could not add company: " + e.getMessage();
            } catch (NumberFormatException e) {
                message = "Could not add company: CGPA and backlogs must be numbers.";
            }
        }
        redirect(ex, "/?msg=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }

    private static void export(HttpExchange ex) throws IOException {
        String message;
        try {
            manager.exportReport("report.txt");
            message = "Report written to report.txt";
        } catch (IOException e) {
            message = "Could not write report.txt";
        }
        redirect(ex, "/?msg=" + URLEncoder.encode(message, StandardCharsets.UTF_8));
    }

    private static void style(HttpExchange ex) throws IOException {
        try {
            byte[] bytes = Files.readAllBytes(Path.of("web", "style.css"));
            ex.getResponseHeaders().set("Content-Type", "text/css; charset=UTF-8");
            ex.sendResponseHeaders(200, bytes.length);
            OutputStream os = ex.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (IOException e) {
            sendText(ex, "web/style.css was not found.");
        }
    }

    private static String readPage(String fileName) throws IOException {
        return Files.readString(Path.of("web", fileName), StandardCharsets.UTF_8);
    }

    private static int countEligible(Company company) {
        int count = 0;
        for (Student s : manager.getAllStudents()) {
            if (company.isEligible(s)) {
                count++;
            }
        }
        return count;
    }

    private static ArrayList<Student> getEligible(String companyName) {
        try {
            return manager.getEligibleStudents(companyName);
        } catch (InvalidDataException e) {
            return new ArrayList<Student>();
        }
    }

    private static void sendHtml(HttpExchange ex, String html) throws IOException {
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void sendText(HttpExchange ex, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        ex.sendResponseHeaders(200, bytes.length);
        OutputStream os = ex.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(303, -1);
        ex.close();
    }

    private static String readBody(HttpExchange ex) throws IOException {
        return new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String text(HashMap<String, String> form, String key) {
        String value = form.get(key);
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static HashMap<String, String> parseForm(String text) {
        HashMap<String, String> map = new HashMap<String, String>();
        if (text == null || text.length() == 0) {
            return map;
        }
        String[] pairs = text.split("&");
        for (String pair : pairs) {
            String[] parts = pair.split("=");
            if (parts.length == 2) {
                map.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        String result = value.replace("&", "&amp;");
        result = result.replace("<", "&lt;");
        result = result.replace(">", "&gt;");
        result = result.replace("\"", "&quot;");
        return result;
    }
}
