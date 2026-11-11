package com.placewise.ui.screens;

import com.placewise.AppContext;
import com.placewise.eligibility.EligibilityResult;
import com.placewise.ui.ConsoleIO;
import com.placewise.ui.Menu;
import java.util.List;

/** Screen 3: one student vs one company, one vs all companies, all students vs one company. */
public final class EligibilityMenu extends Menu {

    public EligibilityMenu(ConsoleIO io, AppContext app) {
        super(io, app);
    }

    @Override
    public void run() {
        int choice;
        do {
            choice = choose("Check eligibility", List.of("One student, one company",
                    "One student, all companies", "All students, one company"));
            switch (choice) {
                case 1 -> oneToOne();
                case 2 -> oneToAll();
                case 3 -> allToOne();
                default -> { }
            }
        } while (choice != 0);
    }

    private void oneToOne() {
        String roll = io.text("Student roll number");
        long companyId = pickCompany().id();
        show(app.eligibility().check(roll, companyId));
    }

    private void oneToAll() {
        List<EligibilityResult> results = app.eligibility().checkAgainstAllCompanies(io.text("Student roll number"));
        if (results.isEmpty()) {
            io.println("There are no companies yet.");
        }
        showAll(results);
        io.println("Eligible for " + countEligible(results) + " of " + results.size() + " companies.");
    }

    private void allToOne() {
        long companyId = pickCompany().id();
        List<EligibilityResult> results = app.eligibility().checkAllStudents(companyId);
        showAll(results);
        io.println("Eligible: " + countEligible(results) + " of " + results.size() + " students.");
    }

    private void showAll(List<EligibilityResult> results) {
        for (EligibilityResult result : results) {
            show(result);
        }
    }

    private static int countEligible(List<EligibilityResult> results) {
        int count = 0;
        for (EligibilityResult result : results) {
            if (result.eligible()) {
                count++;
            }
        }
        return count;
    }

    private void show(EligibilityResult result) {
        io.println(result.student().rollNo() + " (" + result.student().name() + ") vs " + result.company().name()
                + ": " + result.describe());
    }
}
