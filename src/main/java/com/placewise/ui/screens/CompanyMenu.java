package com.placewise.ui.screens;

import com.placewise.AppContext;
import com.placewise.model.Cgpa;
import com.placewise.model.Company;
import com.placewise.ui.ConsoleIO;
import com.placewise.ui.Menu;
import com.placewise.ui.Views;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Screen 2: add, update, delete and list companies and their criteria. */
public final class CompanyMenu extends Menu {

    private static final BigDecimal MAX_PACKAGE = new BigDecimal("1000");

    public CompanyMenu(ConsoleIO io, AppContext app) {
        super(io, app);
    }

    @Override
    public void run() {
        int choice;
        do {
            choice = choose("Companies", List.of("Add a company", "Update a company", "Delete a company",
                    "List companies"));
            switch (choice) {
                case 1 -> add();
                case 2 -> update();
                case 3 -> delete();
                case 4 -> list();
                default -> { }
            }
        } while (choice != 0);
    }

    private static Set<String> parseBranches(String text) {
        Set<String> branches = new HashSet<>();
        for (String branch : text.split("[,;|\\s]+")) {
            if (!branch.isBlank()) {
                branches.add(branch);
            }
        }
        return branches;
    }

    private void add() {
        String name = io.text("Company name");
        BigDecimal minCgpa = io.decimal("Minimum CGPA", Cgpa.MIN, Cgpa.MAX);
        int maxBacklogs = io.integer("Maximum active backlogs allowed", 0, 99);
        Set<String> branches = parseBranches(io.text("Allowed branches, comma separated (Enter = all)", ""));
        BigDecimal pay = io.decimal("Package in LPA", BigDecimal.ZERO, MAX_PACKAGE);
        int seats = io.integer("Seats", 1, 100_000);
        Company saved = app.companies().add(new Company(0, name, minCgpa, maxBacklogs, branches, pay, seats));
        io.println("Added company " + saved.id() + " - " + saved.name() + ".");
    }

    private void update() {
        Company old = pickCompany();
        io.println("Press Enter to keep the current value.");
        Company changed = new Company(old.id(),
                io.text("Name", old.name()),
                io.decimal("Minimum CGPA", Cgpa.MIN, Cgpa.MAX, old.minCgpa()),
                io.integer("Maximum active backlogs allowed", 0, 99, old.maxBacklogs()),
                 updatedBranches(old),
                io.decimal("Package in LPA", BigDecimal.ZERO, MAX_PACKAGE, old.packageLpa()),
                io.integer("Seats", 1, 100_000, old.seats()));
        app.companies().update(changed);
        io.println("Updated company " + changed.id() + ".");
    }

    /** Enter keeps the branches, the word "all" removes the restriction, anything else replaces the list. */
    private Set<String> updatedBranches(Company old) {
        String answer = io.text("Allowed branches (currently " + old.branchesLabel()
                + "; Enter = keep, 'all' = every branch)", "");
        if (answer.isEmpty()) {
            return old.allowedBranches();
        }
        return answer.equalsIgnoreCase("all") ? Set.of() : parseBranches(answer);
    }

    private void delete() {
        Company company = pickCompany();
        if (io.confirm("Delete " + company.name() + " (and its applications)?")) {
            app.companies().delete(company.id());
            io.println("Deleted.");
        } else {
            io.println("Nothing deleted.");
        }
    }

    private void list() {
        List<Company> all = app.companies().listAll();
        io.println(all.isEmpty() ? "There are no companies yet." : Views.companies(all));
    }
}
