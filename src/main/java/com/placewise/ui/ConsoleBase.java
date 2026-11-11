package com.placewise.ui;

import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Scanner;

/**
 * The plumbing of the console: reading raw lines, text prompts and printing. {@link ConsoleIO} adds the
 * number, yes/no and path prompts on top. It is built from an {@link InputStream} and a {@link PrintStream}
 * (not {@code System.in/out}) so tests can feed scripted input and read the output.
 *
 * <p>When the input really ends, {@link #line} throws {@link InputClosedException}; that is the only way a
 * prompt can fail, so a script that runs out of lines can never loop forever.
 */
public abstract class ConsoleBase {

    private final Scanner scanner;
    private final PrintStream out;

    /**
     * Creates the console.
     *
     * @param input  where answers come from
     * @param output where everything is printed
     */
    protected ConsoleBase(InputStream input, PrintStream output) {
        this.scanner = new Scanner(input, StandardCharsets.UTF_8);
        this.out = output;
    }

    /**
     * The output stream, for components (like the drive's progress monitor) that print themselves.
     *
     * @return the stream
     */
    public PrintStream out() {
        return out;
    }

    /**
     * Prints a line.
     *
     * @param text the text
     */
    public void println(String text) {
        out.println(text);
    }

    /** Prints an empty line. */
    public void blank() {
        out.println();
    }

    /**
     * Prints a formatted line.
     *
     * @param format a {@link String#format} pattern
     * @param args   the values
     */
    public void printf(String format, Object... args) {
        out.println(String.format(Locale.ROOT, format, args));
    }

    /**
     * Prompts and reads one raw line.
     *
     * @param prompt the question
     * @return the line without its terminator (may be blank)
     * @throws InputClosedException if the input has ended
     */
    public String line(String prompt) {
        out.print(prompt + ": ");
        out.flush();
        if (!scanner.hasNextLine()) {
            out.println();
            throw new InputClosedException();
        }
        return scanner.nextLine();
    }

    /**
     * Reads a non-blank text, asking again while the answer is blank.
     *
     * @param prompt the question
     * @return the trimmed text
     */
    public String text(String prompt) {
        while (true) {
            String answer = line(prompt).trim();
            if (!answer.isEmpty()) {
                return answer;
            }
            println("This cannot be blank. Try again.");
        }
    }

    /**
     * Reads a text where Enter keeps a default.
     *
     * @param prompt       the question
     * @param defaultValue the value used for a blank answer (may be empty)
     * @return the trimmed answer or the default
     */
    public String text(String prompt, String defaultValue) {
        String answer = line(prompt + (defaultValue.isEmpty() ? "" : " [" + defaultValue + "]")).trim();
        return answer.isEmpty() ? defaultValue : answer;
    }
}
