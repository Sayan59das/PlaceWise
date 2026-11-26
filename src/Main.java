import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;

public class Main {
    private static PlacementManager manager = new PlacementManager();
    private static Scanner sc = new Scanner(System.in);

    public static void main(String[] args) {
        addDefaultCompanies();
        loadDefaultStudents();

        int choice = 0;
        while (choice != 10) {
            showMenu();
            choice = readInt("Enter choice: ");
            System.out.println();
            if (choice == 1) {
                addStudent();
            } else if (choice == 2) {
                addCompany();
            } else if (choice == 3) {
                showAllStudents();
            } else if (choice == 4) {
                showAllCompanies();
            } else if (choice == 5) {
                checkOneStudent();
            } else if (choice == 6) {
                showEligibleList();
            } else if (choice == 7) {
                showRankedList();
            } else if (choice == 8) {
                exportReport();
            } else if (choice == 9) {
                loadFromFile();
            } else if (choice == 10) {
                System.out.println("Exiting program. Goodbye!");
            } else {
                System.out.println("Invalid choice. Try again.");
            }
            System.out.println();
        }
        sc.close();
    }

    private static void showMenu() {
        System.out.println("===== STUDENT PLACEMENT ELIGIBILITY CHECKER =====");
        System.out.println("1. Add Student");
        System.out.println("2. Add Company");
        System.out.println("3. Show All Students");
        System.out.println("4. Show All Companies");
        System.out.println("5. Check One Student For A Company");
        System.out.println("6. Show Eligible Students For A Company");
        System.out.println("7. Show Ranked Students");
        System.out.println("8. Export Placement Report");
        System.out.println("9. Load Students From CSV File");
        System.out.println("10. Exit");
        System.out.println("==================================================");
    }

    private static String readLine() {
        if (sc.hasNextLine()) {
            return sc.nextLine();
        }
        System.out.println();
        System.out.println("Input closed. Exiting program.");
        System.exit(0);
        return "";
    }

    private static int readInt(String message) {
        while (true) {
            System.out.print(message);
            String line = readLine();
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    private static double readDouble(String message) {
        while (true) {
            System.out.print(message);
            String line = readLine();
            try {
                return Double.parseDouble(line.trim());
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid decimal number.");
            }
        }
    }

    private static String readText(String message) {
        System.out.print(message);
        return readLine().trim();
    }

    private static void addDefaultCompanies() {
        try {
            manager.addCompany(new Company("TCS", 6.0, 1));
            manager.addCompany(new Company("Infosys", 7.0, 0));
            manager.addCompany(new Company("Amazon", 8.5, 0));
            manager.addCompany(new Company("Wipro", 6.5, 2));
        } catch (InvalidDataException e) {
            System.out.println("Error adding default company: " + e.getMessage());
        }
    }

    private static void loadDefaultStudents() {
        try {
            int count = manager.loadStudentsFromFile("data/students.csv");
            System.out.println("Loaded " + count + " students from data/students.csv");
        } catch (IOException e) {
            System.out.println("Could not load students.csv: " + e.getMessage());
        }
        System.out.println();
    }

    private static void addStudent() {
        try {
            String rollNo = readText("Enter roll number: ");
            String name = readText("Enter name: ");
            String branch = readText("Enter branch: ");
            double cgpa = readDouble("Enter CGPA: ");
            int backlogs = readInt("Enter number of backlogs: ");

            Student s = new Student(rollNo, name, branch, cgpa, backlogs);
            if (manager.addStudent(s)) {
                System.out.println("Student added successfully.");
            } else {
                System.out.println("A student with this roll number already exists.");
            }
        } catch (InvalidDataException e) {
            System.out.println("Invalid data: " + e.getMessage());
        }
    }

    private static void addCompany() {
        try {
            String name = readText("Enter company name: ");
            double minCgpa = readDouble("Enter minimum CGPA required: ");
            int maxBacklogs = readInt("Enter maximum allowed backlogs: ");

            Company c = new Company(name, minCgpa, maxBacklogs);
            if (manager.addCompany(c)) {
                System.out.println("Company added successfully.");
            } else {
                System.out.println("This company already exists.");
            }
        } catch (InvalidDataException e) {
            System.out.println("Invalid data: " + e.getMessage());
        }
    }

    private static void showAllStudents() {
        ArrayList<Student> students = manager.getAllStudents();
        if (students.isEmpty()) {
            System.out.println("No students found.");
            return;
        }
        System.out.println("Total students: " + Student.getTotalStudents());
        for (Student s : students) {
            Person p = s;
            p.display();
            System.out.println("-----------------------------");
        }
    }

    private static void showAllCompanies() {
        ArrayList<Company> companies = manager.getAllCompanies();
        if (companies.isEmpty()) {
            System.out.println("No companies found.");
            return;
        }
        for (Company c : companies) {
            c.display();
            System.out.println("-----------------------------");
        }
    }

    private static void checkOneStudent() {
        String rollNo = readText("Enter roll number: ");
        String companyName = readText("Enter company name: ");
        try {
            boolean ok = manager.checkEligibility(rollNo, companyName);
            if (ok) {
                System.out.println("Result: ELIGIBLE");
            } else {
                System.out.println("Result: NOT ELIGIBLE");
            }
        } catch (InvalidDataException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void showEligibleList() {
        String companyName = readText("Enter company name: ");
        try {
            ArrayList<Student> eligible = manager.getEligibleStudents(companyName);
            if (eligible.isEmpty()) {
                System.out.println("No eligible students for " + companyName);
                return;
            }
            System.out.println("Eligible students for " + companyName + ":");
            int i = 1;
            for (Student s : eligible) {
                System.out.println(i + ". " + s.getRollNo() + " - " + s.getName()
                        + " - CGPA: " + s.getCgpa()
                        + " - Backlogs: " + s.getBacklogs());
                i++;
            }
        } catch (InvalidDataException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void showRankedList() {
        ArrayList<Student> ranked = manager.getRankedStudents();
        if (ranked.isEmpty()) {
            System.out.println("No students to rank.");
            return;
        }
        System.out.println("Students ranked by CGPA:");
        System.out.println(String.format("%-6s%-12s%-20s%-10s", "Rank", "Roll No", "Name", "CGPA"));
        int rank = 1;
        Iterator<Student> it = ranked.iterator();
        while (it.hasNext()) {
            Student s = it.next();
            System.out.println(String.format("%-6d%-12s%-20s%-10.2f",
                    rank, s.getRollNo(), s.getName(), s.getCgpa()));
            rank++;
        }
    }

    private static void exportReport() {
        String fileName = readText("Enter report file name (example report.txt): ");
        try {
            manager.exportReport(fileName);
            System.out.println("Report exported to " + fileName);
        } catch (IOException e) {
            System.out.println("Could not write report: " + e.getMessage());
        }
    }

    private static void loadFromFile() {
        String fileName = readText("Enter CSV file name (example data/students.csv): ");
        try {
            int count = manager.loadStudentsFromFile(fileName);
            System.out.println("Loaded " + count + " students from " + fileName);
        } catch (IOException e) {
            System.out.println("Could not read file: " + e.getMessage());
        }
    }
}
