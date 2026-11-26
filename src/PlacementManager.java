import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.io.IOException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FileWriter;

public class PlacementManager {
    private ArrayList<Student> students;
    private LinkedHashMap<String, Company> companies;

    public PlacementManager() {
        students = new ArrayList<Student>();
        companies = new LinkedHashMap<String, Company>();
    }

    public boolean addStudent(Student student) {
        if (findStudent(student.getRollNo()) != null) {
            return false;
        }
        students.add(student);
        return true;
    }

    public boolean addCompany(Company company) {
        if (companies.containsKey(company.getName())) {
            return false;
        }
        companies.put(company.getName(), company);
        return true;
    }

    public Student findStudent(String rollNo) {
        for (Student s : students) {
            if (s.getRollNo().equalsIgnoreCase(rollNo)) {
                return s;
            }
        }
        return null;
    }

    public Company findCompany(String companyName) {
        return companies.get(companyName);
    }

    public ArrayList<Student> getAllStudents() {
        return students;
    }

    public ArrayList<Company> getAllCompanies() {
        return new ArrayList<Company>(companies.values());
    }

    public boolean checkEligibility(String rollNo, String companyName) throws InvalidDataException {
        Student student = findStudent(rollNo);
        if (student == null) {
            throw new InvalidDataException("Student not found: " + rollNo);
        }
        Company company = findCompany(companyName);
        if (company == null) {
            throw new InvalidDataException("Company not found: " + companyName);
        }
        EligibilityRule rule = company;
        return rule.isEligible(student);
    }

    public ArrayList<Student> getEligibleStudents(String companyName) throws InvalidDataException {
        Company company = findCompany(companyName);
        if (company == null) {
            throw new InvalidDataException("Company not found: " + companyName);
        }
        return filterEligible(company);
    }

    private ArrayList<Student> filterEligible(Company company) {
        ArrayList<Student> list = new ArrayList<Student>();
        for (Student s : students) {
            if (company.isEligible(s)) {
                list.add(s);
            }
        }
        Collections.sort(list);
        return list;
    }

    public ArrayList<Student> getRankedStudents() {
        ArrayList<Student> ranked = new ArrayList<Student>(students);
        Collections.sort(ranked);
        return ranked;
    }

    public void exportReport(String fileName) throws IOException {
        FileWriter writer = new FileWriter(fileName);
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("STUDENT PLACEMENT REPORT\n");
            sb.append("Total Students: " + students.size() + "\n");
            sb.append("Total Companies: " + companies.size() + "\n");
            sb.append("\nRANKED STUDENT LIST\n");
            sb.append(String.format("%-6s %-12s %-20s %-10s %-8s %-8s%n",
                    "Rank", "Roll No", "Name", "Branch", "CGPA", "Backlogs"));

            ArrayList<Student> ranked = getRankedStudents();
            Iterator<Student> it = ranked.iterator();
            int rank = 1;
            while (it.hasNext()) {
                Student s = it.next();
                sb.append(String.format("%-6d %-12s %-20s %-10s %-8.2f %-8d%n",
                        rank, s.getRollNo(), s.getName(), s.getBranch(),
                        s.getCgpa(), s.getBacklogs()));
                rank++;
            }

            sb.append("\nCOMPANY WISE ELIGIBLE STUDENTS\n");
            for (Company company : getAllCompanies()) {
                sb.append("\nCompany: " + company.getName());
                sb.append(" (Minimum CGPA: " + company.getMinCgpa());
                sb.append(", Maximum Backlogs: " + company.getMaxBacklogs() + ")\n");
                ArrayList<Student> eligible = filterEligible(company);
                sb.append("Eligible Count: " + eligible.size() + "\n");
                for (Student s : eligible) {
                    sb.append("  " + s.getRollNo() + "  " + s.getName()
                            + "  CGPA: " + s.getCgpa()
                            + "  Backlogs: " + s.getBacklogs() + "\n");
                }
            }
            writer.write(sb.toString());
        } finally {
            writer.close();
        }
    }

    public int loadStudentsFromFile(String fileName) throws IOException {
        int count = 0;
        FileReader reader = new FileReader(fileName);
        BufferedReader br = new BufferedReader(reader);
        try {
            String line = br.readLine();
            while (line != null) {
                if (line.trim().length() > 0) {
                    String[] parts = line.split(",");
                    if (parts.length != 5) {
                        System.out.println("Skipped bad line: " + line);
                    } else {
                        try {
                            if (findStudent(parts[0].trim()) != null) {
                                System.out.println("Duplicate roll number skipped: " + parts[0].trim());
                            } else {
                                Student s = new Student(parts[0].trim(), parts[1].trim(),
                                        parts[2].trim(), Double.parseDouble(parts[3].trim()),
                                        Integer.parseInt(parts[4].trim()));
                                addStudent(s);
                                count++;
                            }
                        } catch (InvalidDataException | NumberFormatException e) {
                            System.out.println("Skipped bad line: " + line);
                        }
                    }
                }
                line = br.readLine();
            }
        } finally {
            br.close();
        }
        return count;
    }
}
