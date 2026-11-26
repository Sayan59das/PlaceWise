public class Company implements EligibilityRule {
    private String name;
    private double minCgpa;
    private int maxBacklogs;

    public Company(String name, double minCgpa, int maxBacklogs) throws InvalidDataException {
        if (name == null || name.trim().length() == 0) {
            throw new InvalidDataException("Company name cannot be empty");
        }
        if (minCgpa < 0.0 || minCgpa > 10.0) {
            throw new InvalidDataException("Minimum CGPA must be between 0 and 10");
        }
        if (maxBacklogs < 0) {
            throw new InvalidDataException("Maximum backlogs cannot be negative");
        }
        this.name = name.trim();
        this.minCgpa = minCgpa;
        this.maxBacklogs = maxBacklogs;
    }

    public String getName() {
        return name;
    }

    public double getMinCgpa() {
        return minCgpa;
    }

    public int getMaxBacklogs() {
        return maxBacklogs;
    }

    public boolean isEligible(Student student) {
        if (student.getCgpa() < minCgpa) {
            return false;
        }
        if (student.getBacklogs() > maxBacklogs) {
            return false;
        }
        return true;
    }

    public void display() {
        System.out.println("Company       : " + name);
        System.out.println("Minimum CGPA  : " + minCgpa);
        System.out.println("Max Backlogs  : " + maxBacklogs);
    }
}
