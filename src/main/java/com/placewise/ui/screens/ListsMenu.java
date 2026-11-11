package com.placewise.ui.screens;

import com.placewise.AppContext;
import com.placewise.model.Cgpa;
import com.placewise.model.Company;
import com.placewise.ranking.RankedStudent;
import com.placewise.service.EligibleFilter;
import com.placewise.ui.ConsoleIO;
import com.placewise.ui.Menu;
import com.placewise.ui.Views;
import java.util.List;
import java.util.Locale;

/** Screen 4: eligible and ranked lists of a company, with branch / minimum-CGPA filters, top-N and paging. */
public final class ListsMenu extends Menu {

    public ListsMenu(ConsoleIO io, AppContext app) {
        super(io, app);
    }

    @Override
    public void run() {
        Company company = pickCompany();
        EligibleFilter filter = new EligibleFilter(io.text("Only this branch (Enter = any)", ""),
                io.optionalDecimal("Only students with at least this CGPA", Cgpa.MIN, Cgpa.MAX).orElse(null));
        int choice;
        do {
            choice = choose("Lists for " + company.name(), List.of("Eligible students", "Ranked eligible students",
                    "Top N", "Browse page by page"));
            switch (choice) {
                case 1 -> eligible(company, filter);
                case 2 -> ranked(company, filter);
                case 3 -> top(company, filter);
                case 4 -> pages(company, filter);
                default -> { }
            }
        } while (choice != 0);
    }

    private void eligible(Company company, EligibleFilter filter) {
        var students = app.listings().eligibleStudents(company.id(), filter);
        io.println(students.isEmpty() ? "No eligible students match."
                : Views.students("Eligible for " + company.name(), students));
    }

    private void ranked(Company company, EligibleFilter filter) {
        List<RankedStudent> ranked = app.listings().ranked(company.id(), filter);
        io.println(ranked.isEmpty() ? "No eligible students match."
                : Views.ranked("Ranked for " + company.name(), ranked));
    }

    private void top(Company company, EligibleFilter filter) {
        int n = io.integer("How many top students", 1, 100_000);
        List<RankedStudent> top = app.listings().top(company.id(), filter, n);
        io.println(top.isEmpty() ? "No eligible students match." : Views.ranked("Top " + n + " for " + company.name(), top));
    }

    private void pages(Company company, EligibleFilter filter) {
        int size = io.integer("Students per page", 1, 1_000, 10);
        int page = 1;
        while (true) {
            List<RankedStudent> students = app.listings().page(company.id(), filter, page, size);
            if (students.isEmpty()) {
                io.println(page == 1 ? "No eligible students match." : "That page is empty.");
                if (page == 1) {
                    return;
                }
                page--;
            } else {
                io.println(Views.ranked("Page " + page + " for " + company.name(), students));
            }
            String command = io.text("[n]ext, [p]revious, [q]uit", "q").toLowerCase(Locale.ROOT);
            if (command.startsWith("n")) {
                page++;
            } else if (command.startsWith("p")) {
                page = Math.max(1, page - 1);
            } else if (command.startsWith("q")) {
                return;
            } else {
                io.println("Please type n, p or q.");
            }
        }
    }
}
