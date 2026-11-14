package com.placewise;

import java.nio.file.Path;

/**
 * Command-line options of {@link Main}.
 *
 * @param noDb     {@code true} to keep everything in memory
 * @param dbFile   a specific {@code db.properties} file, or {@code null} to look for one in the current folder
 * @param dataDir  folder for the audit log, backup, reports and rejects
 * @param help     {@code true} if usage should be printed
 * @param web      {@code true} to start the browser version instead of the terminal menu
 * @param port     the port of the browser version (0 picks any free port)
 */
public record Options(boolean noDb, Path dbFile, Path dataDir, boolean help, boolean web, int port) {

    /** The port used when {@code --port} is not given. */
    public static final int DEFAULT_PORT = 8080;

    /** The usage text. */
    public static final String USAGE = """
            Usage: java -jar placewise.jar [options]
              --db FILE        use the MySQL settings in FILE (default: ./db.properties if it exists)
              --no-db          keep everything in memory (nothing is saved)
              --data-dir DIR   folder for audit log, reports, rejects (default: placewise-data)
              --web            start the browser version instead of the terminal menu
              --port N         port of the browser version (default: 8080)
              --help           show this text""";

    /**
     * Parses arguments.
     *
     * @param args the command line
     * @return the options
     * @throws IllegalArgumentException with a readable message for an unknown option or a missing value
     */
    public static Options parse(String[] args) {
        boolean noDb = false;
        boolean help = false;
        boolean web = false;
        int port = DEFAULT_PORT;
        Path dbFile = null;
        Path dataDir = Path.of("placewise-data");
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--no-db" -> noDb = true;
                case "--help", "-h" -> help = true;
                case "--web" -> web = true;
                case "--port" -> port = port(value(args, ++i, "--port"));
                case "--db" -> dbFile = Path.of(value(args, ++i, "--db"));
                case "--data-dir" -> dataDir = Path.of(value(args, ++i, "--data-dir"));
                default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }
        if (noDb && dbFile != null) {
            throw new IllegalArgumentException("Use either --db or --no-db, not both");
        }
        return new Options(noDb, dbFile, dataDir, help, web, port);
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " needs a value");
        }
        return args[index];
    }

    private static int port(String text) {
        try {
            int port = Integer.parseInt(text);
            if (port >= 0 && port <= 65_535) {
                return port;
            }
        } catch (NumberFormatException e) {
            // fall through to the message below
        }
        throw new IllegalArgumentException("--port needs a number between 0 and 65535");
    }
}
