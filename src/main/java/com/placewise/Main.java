package com.placewise;

import com.placewise.exception.PlaceWiseException;
import com.placewise.storage.database.Database;
import com.placewise.storage.database.DbConfig;
import com.placewise.ui.ConsoleIO;
import com.placewise.ui.MainMenu;
import com.placewise.web.WebServer;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Program entry point: chooses the storage, starts the terminal menu (or, with {@code --web}, the browser
 * version), and shuts everything down cleanly.
 */
public final class Main {

    private Main() {
    }

    /**
     * Starts PlaceWise.
     *
     * @param args see {@link Options#USAGE}
     */
    public static void main(String[] args) {
        System.exit(run(args, System.in, System.out));
    }

    /**
     * Runs the program with injectable streams (this is what the tests call).
     *
     * @param args   command-line arguments
     * @param in     where the user's answers come from
     * @param out    where output goes
     * @return the process exit code: 0 on a normal exit, 2 for bad arguments or an unusable database
     */
    public static int run(String[] args, InputStream in, PrintStream out) {
        Options options;
        try {
            options = Options.parse(args);
        } catch (IllegalArgumentException e) {
            out.println(e.getMessage());
            out.println(Options.USAGE);
            return 2;
        }
        if (options.help()) {
            out.println(Options.USAGE);
            return 0;
        }
        Database database = null;
        try {
            database = openDatabase(options, out);
            try (AppContext app = AppContext.create(database, options.dataDir())) {
                out.println("PlaceWise - storage: " + app.storageDescription() + "; data folder: " + options.dataDir());
                if (options.web()) {
                    WebServer.runUntilEnter(app, options.port(), in, out); // the browser version
                } else {
                    new MainMenu(new ConsoleIO(in, out), app).run();      // the terminal version
                }
                out.println("Audit log backed up to " + app.backupAudit());
            }
            return 0;
        } catch (PlaceWiseException e) {
            out.println("Cannot continue: " + e.getMessage());
            if (!options.noDb()) {
                out.println("Tip: run with --no-db to try the program without a database.");
            }
            return 2;
        } finally {
            if (database != null) {
                database.close(); // also covered by AppContext.close(); closing twice is harmless
            }
        }
    }

    private static Database openDatabase(Options options, PrintStream out) {
        if (options.noDb()) {
            return null;
        }
        Path file = options.dbFile() != null ? options.dbFile() : Path.of("db.properties");
        if (options.dbFile() == null && !Files.exists(file)) {
            out.println("No db.properties found: running with in-memory storage (copy db.properties.example "
                    + "to db.properties to use MySQL).");
            return null;
        }
        Database database = new Database(DbConfig.load(file));
        database.verify(); // fail now, with a clear message, not in the middle of a menu action
        return database;
    }
}
