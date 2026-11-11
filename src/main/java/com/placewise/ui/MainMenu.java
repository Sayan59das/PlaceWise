package com.placewise.ui;

import com.placewise.AppContext;
import com.placewise.exception.PlaceWiseException;
import com.placewise.ui.screens.CompanyMenu;
import com.placewise.ui.screens.DriveMenu;
import com.placewise.ui.screens.EligibilityMenu;
import com.placewise.ui.screens.ImportMenu;
import com.placewise.ui.screens.InsightsMenu;
import com.placewise.ui.screens.ListsMenu;
import com.placewise.ui.screens.ReportMenu;
import com.placewise.ui.screens.StudentMenu;

/**
 * The top-level menu. The loop is the program's safety net: a business error ({@link PlaceWiseException})
 * prints its message and returns to this menu; an unexpected exception prints a short note and also
 * returns here; only a real end of input (or choosing 0) leaves the loop.
 */
public final class MainMenu {

    private final ConsoleIO io;
    private final StudentMenu students;
    private final CompanyMenu companies;
    private final EligibilityMenu eligibility;
    private final ListsMenu lists;
    private final InsightsMenu insights;
    private final DriveMenu drive;
    private final ReportMenu reports;
    private final ImportMenu imports;

    /**
     * Creates the menu.
     *
     * @param io  console (inject scripted streams in tests)
     * @param app the application services
     */
    public MainMenu(ConsoleIO io, AppContext app) {
        this.io = io;
        this.students = new StudentMenu(io, app);
        this.companies = new CompanyMenu(io, app);
        this.eligibility = new EligibilityMenu(io, app);
        this.lists = new ListsMenu(io, app);
        this.insights = new InsightsMenu(io, app);
        this.drive = new DriveMenu(io, app);
        this.reports = new ReportMenu(io, app);
        this.imports = new ImportMenu(io, app);
    }

    /** Runs until the user chooses 0 or the input ends. */
    public void run() {
        try {
            int choice;
            do {
                printMenu();
                choice = io.integer("Choose", 0, 9);
                dispatch(choice);
            } while (choice != 0);
        } catch (InputClosedException e) {
            io.println("Input ended - shutting down.");
        }
        io.println("Goodbye.");
    }

    private void printMenu() {
        io.blank();
        io.println("===== PlaceWise - Student Placement Eligibility Checker =====");
        io.println(" 1. Students");
        io.println(" 2. Companies");
        io.println(" 3. Check eligibility");
        io.println(" 4. Eligible and ranked lists");
        io.println(" 5. Explain why a student is not eligible");
        io.println(" 6. What-if simulator");
        io.println(" 7. Placement drive simulation (threads)");
        io.println(" 8. Reports and export");
        io.println(" 9. Import CSV");
        io.println(" 0. Exit");
    }

    private void dispatch(int choice) {
        try {
            switch (choice) {
                case 1 -> students.run();
                case 2 -> companies.run();
                case 3 -> eligibility.run();
                case 4 -> lists.run();
                case 5 -> insights.explain();
                case 6 -> insights.whatIf();
                case 7 -> drive.run();
                case 8 -> reports.run();
                case 9 -> imports.run();
                default -> { }
            }
        } catch (PlaceWiseException e) {
            io.println("Error: " + e.getMessage());
        } catch (InputClosedException e) {
            throw e; // not an error: the input is over, let run() shut down
        } catch (RuntimeException e) {
            io.println("Unexpected problem (" + e.getClass().getSimpleName() + "): " + e.getMessage());
        }
    }
}
