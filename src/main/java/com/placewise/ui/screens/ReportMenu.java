package com.placewise.ui.screens;

import com.placewise.AppContext;
import com.placewise.report.CsvReportRenderer;
import com.placewise.report.ReportRenderer;
import com.placewise.report.Table;
import com.placewise.report.TextReportRenderer;
import com.placewise.ui.ConsoleIO;
import com.placewise.ui.Menu;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/** Screen 8: build the placement reports, show them, and export them as text or CSV. */
public final class ReportMenu extends Menu {

    public ReportMenu(ConsoleIO io, AppContext app) {
        super(io, app);
    }

    @Override
    public void run() {
        int choice;
        do {
            choice = choose("Reports and export", List.of("Eligible list of a company",
                    "Ranked list of a company", "Branch-wise summary", "Overall placement summary",
                    "Export EVERY report (text and CSV)", "Export all students as CSV",
                    "Export all companies as CSV", "Back up the audit log"));
            switch (choice) {
                case 1 -> single(app.reports().eligibleTable(pickCompany().id()));
                case 2 -> single(app.reports().rankedTable(pickCompany().id()));
                case 3 -> single(app.reports().branchTable());
                case 4 -> single(app.reports().overallTable());
                case 5 -> everything();
                case 6 -> exportStudents();
                case 7 -> exportCompanies();
                case 8 -> io.println("Audit log copied to " + app.backupAudit());
                default -> { }
            }
        } while (choice != 0);
    }

    private Path reportDir() {
        return io.path("Folder for the report", app.dataDir().resolve("reports"));
    }

    /** Shows a report on screen and optionally writes it to a file. */
    private void single(Table table) {
        io.println(new TextReportRenderer().render(table));
        int format = io.integer("Export as 1 = text file, 2 = CSV file, 0 = just show", 0, 2, 0);
        if (format == 0) {
            return;
        }
        ReportRenderer renderer = format == 2 ? new CsvReportRenderer() : new TextReportRenderer();
        String baseName = table.title().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-");
        Path file = app.reports().export(table, renderer, reportDir(), baseName);
        io.println("Wrote " + file);
    }

    private void everything() {
        List<Path> files = app.reports().exportAll(reportDir());
        io.println("Wrote " + files.size() + " files:");
        for (Path file : files) {
            io.println("  " + file);
        }
    }

    private void exportStudents() {
        Path file = io.path("Target CSV file", app.dataDir().resolve("students-export.csv"));
        io.println("Wrote " + app.reports().exportStudents(file) + " students to " + file);
    }

    private void exportCompanies() {
        Path file = io.path("Target CSV file", app.dataDir().resolve("companies-export.csv"));
        io.println("Wrote " + app.reports().exportCompanies(file) + " companies to " + file);
    }
}
