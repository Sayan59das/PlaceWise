package com.placewise;

import java.nio.file.Path;

/**
 * Command-line options of {@link Main}.
 *
 * @param noDb     {@code true} to keep everything in memory
 * @param dbFile   a specific {@code db.properties} file, or {@code null} to look for one in the current folder
 * @param dataDir  folder for the audit log, backup, reports and rejects
 * @param help     {@code true} if usage should be printed
 */
public record Options(boolean noDb, Path dbFile, Path dataDir, boolean help) {

    /** The usage text. */
    public static final String USAGE = """
            Usage: java -jar placewise.jar [options]
              --db FILE        use the MySQL settings in FILE (default: ./db.properties if it exists)
              --no-db          keep everything in memory (nothing is saved)
              --data-dir DIR   folder for audit log, reports, rejects (default: placewise-data)
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
        Path dbFile = null;
        Path dataDir = Path.of("placewise-data");
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--no-db" -> noDb = true;
                case "--help", "-h" -> help = true;
                case "--db" -> dbFile = Path.of(value(args, ++i, "--db"));
                case "--data-dir" -> dataDir = Path.of(value(args, ++i, "--data-dir"));
                default -> throw new IllegalArgumentException("Unknown option: " + args[i]);
            }
        }
        if (noDb && dbFile != null) {
            throw new IllegalArgumentException("Use either --db or --no-db, not both");
        }
        return new Options(noDb, dbFile, dataDir, help);
    }

    private static String value(String[] args, int index, String option) {
        if (index >= args.length) {
            throw new IllegalArgumentException(option + " needs a value");
        }
        return args[index];
    }
}
