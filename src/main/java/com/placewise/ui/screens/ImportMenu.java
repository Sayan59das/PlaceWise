package com.placewise.ui.screens;

import com.placewise.AppContext;
import com.placewise.service.ImportSummary;
import com.placewise.ui.ConsoleIO;
import com.placewise.ui.Menu;
import java.nio.file.Path;
import java.util.List;

/** Screen 9: import students or companies from CSV; bad rows go to {@code rejects.csv}. */
public final class ImportMenu extends Menu {

    public ImportMenu(ConsoleIO io, AppContext app) {
        super(io, app);
    }

    @Override
    public void run() {
        int choice;
        do {
            choice = choose("Import CSV", List.of("Import students", "Import companies"));
            switch (choice) {
                case 1 -> importFile("students", Path.of("sample-data", "students.csv"), true);
                case 2 -> importFile("companies", Path.of("sample-data", "companies.csv"), false);
                default -> { }
            }
        } while (choice != 0);
    }

    private void importFile(String what, Path defaultFile, boolean students) {
        Path file = io.path("CSV file with " + what, defaultFile);
        Path rejects = io.path("Where to write rejected rows", app.dataDir().resolve("rejects.csv"));
        ImportSummary summary = students ? app.imports().importStudents(file, rejects)
                : app.imports().importCompanies(file, rejects);
        io.println(summary.describe());
    }
}
