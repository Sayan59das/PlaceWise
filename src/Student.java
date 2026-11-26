public class Student extends Person implements Comparable<Student> {
    private String rollNo;
    private String branch;
    private double cgpa;
    private int backlogs;
    private static int totalStudents = 0;

    public Student(String rollNo, String name, String branch, double cgpa, int backlogs)
            throws InvalidDataException {
        super(name);
        if (rollNo == null || rollNo.trim().length() == 0) {
            throw new InvalidDataException("Roll number cannot be empty");
        }
        if (branch == null || branch.trim().length() == 0) {
            throw new InvalidDataException("Branch cannot be empty");
        }
        if (cgpa < 0.0 || cgpa > 10.0) {
            throw new InvalidDataException("CGPA must be between 0 and 10");
        }
        if (backlogs < 0) {
            throw new InvalidDataException("Backlogs cannot be negative");
        }
        this.rollNo = rollNo.trim();
        this.branch = branch.trim();
        this.cgpa = cgpa;
        this.backlogs = backlogs;
        totalStudents++;
    }

    public String getRollNo() {
        return rollNo;
    }

    public String getBranch() {
        return branch;
    }

    public double getCgpa() {
        return cgpa;
    }

    public int getBacklogs() {
        return backlogs;
    }

    public static int getTotalStudents() {
        return totalStudents;
    }

    public int compareTo(Student other) {
        if (this.cgpa > other.cgpa) {
            return -1;
        } else if (this.cgpa < other.cgpa) {
            return 1;
        } else {
            return this.rollNo.compareTo(other.rollNo);
        }
    }

    public void display() {
        System.out.println("Roll No : " + rollNo);
        System.out.println("Name    : " + getName());
        System.out.println("Branch  : " + branch);
        System.out.println("CGPA    : " + cgpa);
        System.out.println("Backlogs: " + backlogs);
    }
}
