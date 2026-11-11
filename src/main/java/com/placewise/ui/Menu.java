package com.placewise.ui;

import com.placewise.AppContext;
import com.placewise.exception.PlaceWiseException;
import com.placewise.model.Company;
import java.util.List;

/**
 * Base class of every menu screen: it holds the console and the application services and offers the
 * helpers all screens need. Subclasses implement {@link #run()}.
 */
public abstract class Menu {

    protected final ConsoleIO io;
    protected final AppContext app;

    protected Menu(ConsoleIO io, AppContext app) {
        this.io = io;
        this.app = app;
    }

    /** Shows the screen and handles choices until the user goes back. */
    public abstract void run();

    /**
     * Prints a numbered sub-menu and reads the choice.
     *
     * @param title   the menu title
     * @param options the entries, numbered from 1; 0 is always "Back"
     * @return the chosen number, 0 for back
     */
    protected int choose(String title, List<String> options) {
        io.blank();
        io.println("--- " + title + " ---");
        for (int i = 0; i < options.size(); i++) {
            io.println(" " + (i + 1) + ". " + options.get(i));
        }
        io.println(" 0. Back");
        return io.integer("Choose", 0, options.size());
    }

    /**
     * Shows the companies and lets the user pick one by id.
     *
     * @return the chosen company
     * @throws PlaceWiseException if there are no companies or the id does not exist
     */
    protected Company pickCompany() {
        List<Company> companies = app.companies().listAll();
        if (companies.isEmpty()) {
            throw new PlaceWiseException("There are no companies yet - add one first");
        }
        io.println(Views.companies(companies));
        return app.companies().get(io.integer("Company id", 1, Integer.MAX_VALUE));
    }
}
