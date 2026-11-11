package com.placewise.ui.screens;

import com.placewise.AppContext;
import com.placewise.analytics.Explanation;
import com.placewise.analytics.WhatIfResult;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.StudentSort;
import com.placewise.ui.ConsoleIO;
import com.placewise.ui.Menu;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Screens 5 and 6: "why not eligible?" and the what-if simulator. */
public final class InsightsMenu extends Menu {

    private static final BigDecimal TEN = new BigDecimal("10.00");

    public InsightsMenu(ConsoleIO io, AppContext app) {
        super(io, app);
    }

    @Override
    public void run() {
        explain();
    }

    /** Lists every company that rejects the student, with the exact gap on every failed rule. */
    public void explain() {
        Student student = app.students().get(io.text("Student roll number"));
        List<Company> companies = app.companies().listAll();
        if (companies.isEmpty()) {
            io.println("There are no companies yet.");
            return;
        }
        io.println("Why " + student.rollNo() + " (" + student.name() + ", CGPA " + student.cgpa() + ", "
                + student.activeBacklogs() + " backlog(s), " + student.branch() + ") is rejected:");
        List<Explanation> rejections = app.explainer().explainRejections(student, companies);
        if (rejections.isEmpty()) {
            io.println("Not rejected anywhere: eligible for all " + companies.size() + " companies.");
        }
        for (Explanation explanation : rejections) {
            io.println("  " + explanation.company().name() + ": " + explanation.summary());
        }
        long eligible = companies.size() - rejections.size();
        if (eligible > 0 && !rejections.isEmpty()) {
            io.println("Eligible for the other " + eligible + " company(ies).");
        }
    }

    /** Asks "if my CGPA rose by X and I cleared Y backlogs, what opens up?" without changing any data. */
    public void whatIf() {
        Student student = app.students().get(io.text("Student roll number"));
        BigDecimal increase = io.decimal("CGPA increase", BigDecimal.ZERO, TEN, BigDecimal.ZERO);
        int cleared = io.integer("Backlogs cleared", 0, 99, 0);
        List<Company> companies = app.companies().listAll();
        WhatIfResult result = app.whatIf().simulate(student, companies, increase, cleared);
        io.println("Scenario: CGPA " + student.cgpa() + " -> " + result.scenario().cgpa() + ", backlogs "
                + student.activeBacklogs() + " -> " + result.scenario().activeBacklogs()
                + " (your stored record is not changed).");
        io.println("Already eligible: " + names(result.alreadyEligible()));
        io.println("Newly unlocked:   " + names(result.newlyUnlocked()));
        io.println("Still blocked:    " + names(result.stillBlocked()));
        if (!companies.isEmpty() && io.confirm("Also show how many students would unlock each company?")) {
            Map<String, Long> unlocks = app.whatIf().cohortUnlocks(app.students().listAll(StudentSort.ROLL_NO),
                    companies, increase, cleared);
            for (Map.Entry<String, Long> entry : unlocks.entrySet()) {
                io.println("  " + entry.getKey() + ": " + entry.getValue() + " newly eligible student(s)");
            }
        }
    }

    private static String names(List<Company> companies) {
        if (companies.isEmpty()) {
            return "(none)";
        }
        List<String> names = new ArrayList<>();
        for (Company company : companies) {
            names.add(company.name());
        }
        return String.join(", ", names);
    }
}
