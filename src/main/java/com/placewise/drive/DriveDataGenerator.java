package com.placewise.drive;

import com.placewise.model.Company;
import com.placewise.model.Student;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Creates reproducible random students and companies for simulations (same seed, same data). */
public final class DriveDataGenerator {

    private static final String[] BRANCHES = {"CSE", "IT", "ECE", "ME", "CE"};
    private static final String[] FIRST_NAMES = {"Aarav", "Diya", "Kabir", "Meera", "Rohan", "Sara", "Vikram",
            "Anaya", "Ishaan", "Neha", "Arjun", "Pooja", "Dev", "Riya", "Karan", "Tara"};
    private static final String[] COMPANY_NAMES = {"Acme Tech", "Bluepeak Systems", "Cedar Analytics",
            "Delta Robotics", "Evergreen Cloud", "Fathom Labs", "Granite Finance", "Helix Health"};

    private DriveDataGenerator() {
    }

    /**
     * Generates students. CGPA moves in steps of 0.05, so ties are common and the ranking tie-breakers
     * (backlogs, name, roll number) get real work.
     *
     * @param count how many students
     * @param seed  random seed
     * @return students with roll numbers GEN0001, GEN0002, ...
     */
    public static List<Student> students(int count, long seed) {
        Random random = new Random(seed);
        List<Student> students = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            BigDecimal cgpa = BigDecimal.valueOf(500 + 5L * random.nextInt(101), 2); // 5.00 .. 10.00
            int backlogs = random.nextInt(10) < 7 ? 0 : 1 + random.nextInt(3);
            students.add(new Student(String.format("GEN%04d", i),
                    FIRST_NAMES[random.nextInt(FIRST_NAMES.length)] + " " + (char) ('A' + random.nextInt(26)),
                    BRANCHES[random.nextInt(BRANCHES.length)], 1 + random.nextInt(8), cgpa, backlogs, null));
        }
        return students;
    }

    /**
     * Generates companies with ids 1..count.
     *
     * @param count          how many companies
     * @param seatsPerCompany seats of each
     * @param seed           random seed
     * @return companies with moderate criteria, so many (but not all) students qualify
     */
    public static List<Company> companies(int count, int seatsPerCompany, long seed) {
        Random random = new Random(seed ^ 0x5DEECE66DL);
        List<Company> companies = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String name = i < COMPANY_NAMES.length ? COMPANY_NAMES[i] : "Company " + (i + 1);
            Set<String> branches = new HashSet<>();
            if (random.nextBoolean()) { // half the companies take every branch
                int howMany = 2 + random.nextInt(2);
                while (branches.size() < howMany) {
                    branches.add(BRANCHES[random.nextInt(BRANCHES.length)]);
                }
            }
            companies.add(new Company(i + 1L, name, BigDecimal.valueOf(600 + 25L * random.nextInt(7), 2),
                    random.nextInt(3), branches, BigDecimal.valueOf(400 + 50L * random.nextInt(53), 2),
                    seatsPerCompany));
        }
        return companies;
    }
}
