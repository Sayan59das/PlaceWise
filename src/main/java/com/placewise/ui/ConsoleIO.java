package com.placewise.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * All keyboard input and screen output of the program (see {@link ConsoleBase} for the plumbing).
 *
 * <p>Every read method <em>re-prompts</em> on bad input - blank lines, text where a number is expected,
 * numbers out of range - and never throws for them. The only exception is {@link InputClosedException}
 * when the input really ends.
 */
public final class ConsoleIO extends ConsoleBase {

    /**
     * Creates the console.
     *
     * @param input  where answers come from
     * @param output where everything is printed
     */
    public ConsoleIO(InputStream input, PrintStream output) {
        super(input, output);
    }

    /**
     * Reads a whole number in a range.
     *
     * @param prompt the question
     * @param min    smallest allowed value
     * @param max    largest allowed value
     * @return the number
     */
    public int integer(String prompt, int min, int max) {
        return readInteger(prompt, min, max, null);
    }

    /**
     * Reads a whole number in a range where Enter keeps a default.
     *
     * @param prompt       the question
     * @param min          smallest allowed value
     * @param max          largest allowed value
     * @param defaultValue the value used for a blank answer
     * @return the number
     */
    public int integer(String prompt, int min, int max, int defaultValue) {
        return readInteger(prompt, min, max, defaultValue);
    }

    private int readInteger(String prompt, int min, int max, Integer defaultValue) {
        String range = max < 1_000_000 ? min + "-" + max : "at least " + min;
        String suffix = " (" + range + (defaultValue == null ? "" : ", Enter = " + defaultValue) + ")";
        while (true) {
            String answer = line(prompt + suffix).trim();
            if (answer.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            try {
                int value = Integer.parseInt(answer);
                if (value >= min && value <= max) {
                    return value;
                }
                println("Please enter a number in the range " + range + ".");
            } catch (NumberFormatException e) {
                println(answer.isEmpty() ? "Please type a number." : "'" + answer + "' is not a whole number.");
            }
        }
    }

    /**
     * Reads a whole number of any size where Enter keeps a default (used for random seeds).
     *
     * @param prompt       the question
     * @param defaultValue the value used for a blank answer
     * @return the number
     */
    public long number(String prompt, long defaultValue) {
        while (true) {
            String answer = line(prompt + " (Enter = " + defaultValue + ")").trim();
            if (answer.isEmpty()) {
                return defaultValue;
            }
            try {
                return Long.parseLong(answer);
            } catch (NumberFormatException e) {
                println("'" + answer + "' is not a whole number.");
            }
        }
    }

    /**
     * Reads a decimal number in a range.
     *
     * @param prompt the question
     * @param min    smallest allowed value
     * @param max    largest allowed value
     * @return the number
     */
    public BigDecimal decimal(String prompt, BigDecimal min, BigDecimal max) {
        while (true) {
            Optional<BigDecimal> value = parseDecimal(line(prompt + " (" + min + "-" + max + ")").trim(), min, max);
            if (value.isPresent()) {
                return value.get();
            }
        }
    }

    /**
     * Reads a decimal number in a range where Enter keeps a default.
     *
     * @param prompt       the question
     * @param min          smallest allowed value
     * @param max          largest allowed value
     * @param defaultValue the value used for a blank answer
     * @return the number
     */
    public BigDecimal decimal(String prompt, BigDecimal min, BigDecimal max, BigDecimal defaultValue) {
        while (true) {
            String answer = line(prompt + " (" + min + "-" + max + ", Enter = " + defaultValue + ")").trim();
            if (answer.isEmpty()) {
                return defaultValue;
            }
            Optional<BigDecimal> value = parseDecimal(answer, min, max);
            if (value.isPresent()) {
                return value.get();
            }
        }
    }

    /**
     * Reads an optional decimal number; Enter means "none".
     *
     * @param prompt the question
     * @param min    smallest allowed value
     * @param max    largest allowed value
     * @return the number, or empty for a blank answer
     */
    public Optional<BigDecimal> optionalDecimal(String prompt, BigDecimal min, BigDecimal max) {
        while (true) {
            String answer = line(prompt + " (" + min + "-" + max + ", Enter = none)").trim();
            if (answer.isEmpty()) {
                return Optional.empty();
            }
            Optional<BigDecimal> value = parseDecimal(answer, min, max);
            if (value.isPresent()) {
                return value;
            }
        }
    }

    private Optional<BigDecimal> parseDecimal(String answer, BigDecimal min, BigDecimal max) {
        try {
            BigDecimal value = new BigDecimal(answer);
            // refuse absurd sizes such as 1e999999999 before they can cost memory
            if (value.precision() - value.scale() > 9 || value.scale() > 20) {
                println("That number is too large or too precise.");
            } else if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
                println("Please enter a value between " + min + " and " + max + ".");
            } else {
                return Optional.of(value);
            }
        } catch (NumberFormatException e) {
            println(answer.isEmpty() ? "Please type a number." : "'" + answer + "' is not a number.");
        }
        return Optional.empty();
    }

    /**
     * Asks a yes/no question.
     *
     * @param prompt the question
     * @return {@code true} for y/yes, {@code false} for n/no
     */
    public boolean confirm(String prompt) {
        while (true) {
            String answer = line(prompt + " (y/n)").trim().toLowerCase(Locale.ROOT);
            if (answer.equals("y") || answer.equals("yes")) {
                return true;
            }
            if (answer.equals("n") || answer.equals("no")) {
                return false;
            }
            println("Please answer y or n.");
        }
    }

    /**
     * Reads a file or folder path where Enter keeps a default.
     *
     * @param prompt       the question
     * @param defaultValue the default path
     * @return the path
     */
    public Path path(String prompt, Path defaultValue) {
        return Path.of(text(prompt, defaultValue.toString()));
    }
}
