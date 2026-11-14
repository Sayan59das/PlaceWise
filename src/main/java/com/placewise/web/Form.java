package com.placewise.web;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * The values the web page sent, such as {@code rollNo=CU2501&cgpa=8.5}. They arrive in the address for a
 * GET request and in the body for a POST request; both use the same {@code name=value&name=value} format.
 *
 * <p>This class plays the role {@code ConsoleIO} plays for the terminal: it turns text into numbers and says
 * clearly what is wrong when it cannot. It does not check ranges (CGPA 0-10, semester 1-8, ...): the
 * validators and config records already do that, and they report every problem at once.
 */
public final class Form {

    private final Map<String, String> values;

    private Form(Map<String, String> values) {
        this.values = values;
    }

    /**
     * Parses {@code name=value&name=value} text. Escapes such as {@code %20} and {@code +} are decoded.
     *
     * @param encoded the raw text, or {@code null} when there is none
     * @return the parsed values
     * @throws IllegalArgumentException if an escape is broken (for example {@code %G1})
     */
    public static Form parse(String encoded) {
        Map<String, String> values = new HashMap<>();
        if (encoded == null || encoded.isEmpty()) {
            return new Form(values);
        }
        for (String pair : encoded.split("&")) {
            int equals = pair.indexOf('=');
            String name = equals < 0 ? pair : pair.substring(0, equals);
            String value = equals < 0 ? "" : pair.substring(equals + 1);
            values.put(decode(name), decode(value));
        }
        return new Form(values);
    }

    private static String decode(String text) {
        return URLDecoder.decode(text, StandardCharsets.UTF_8);
    }

    /**
     * A text field.
     *
     * @param name the field name
     * @return the trimmed text, or {@code ""} when the page did not send the field
     */
    public String text(String name) {
        return values.getOrDefault(name, "").trim();
    }

    /**
     * A whole number that must be filled in.
     *
     * @param name the field name
     * @return the number
     * @throws IllegalArgumentException if the field is blank or not a whole number
     */
    public int integer(String name) {
        String text = text(name);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(problem(name, text, "a whole number"));
        }
    }

    /**
     * A whole number, or a default when the field is blank.
     *
     * @param name         the field name
     * @param defaultValue used when the field is blank or missing
     * @return the number
     */
    public int integer(String name, int defaultValue) {
        return text(name).isEmpty() ? defaultValue : integer(name);
    }

    /**
     * A large whole number (company ids, random seeds) that must be filled in.
     *
     * @param name the field name
     * @return the number
     * @throws IllegalArgumentException if the field is blank or not a whole number
     */
    public long number(String name) {
        String text = text(name);
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(problem(name, text, "a whole number"));
        }
    }

    /**
     * A large whole number, or a default when the field is blank.
     *
     * @param name         the field name
     * @param defaultValue used when the field is blank or missing
     * @return the number
     */
    public long number(String name, long defaultValue) {
        return text(name).isEmpty() ? defaultValue : number(name);
    }

    /**
     * A decimal number such as {@code 7.85} that must be filled in.
     *
     * @param name the field name
     * @return the number
     * @throws IllegalArgumentException if the field is blank, not a number, or absurdly large
     */
    public BigDecimal decimal(String name) {
        String text = text(name);
        BigDecimal value;
        try {
            value = new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(problem(name, text, "a number"));
        }
        // the same guard as ConsoleIO: refuse sizes such as 1e999999999 before they can cost memory
        if (value.precision() - value.scale() > 9 || value.scale() > 20) {
            throw new IllegalArgumentException("'" + text + "' is too large or too precise (" + name + ")");
        }
        return value;
    }

    /**
     * A decimal number, or a default when the field is blank.
     *
     * @param name         the field name
     * @param defaultValue used when the field is blank or missing ({@code null} is allowed)
     * @return the number, or the default
     */
    public BigDecimal decimal(String name, BigDecimal defaultValue) {
        return text(name).isEmpty() ? defaultValue : decimal(name);
    }

    private static String problem(String name, String text, String expected) {
        if (text.isEmpty()) {
            return "Please fill in " + name;
        }
        return "'" + text + "' is not " + expected + " (" + name + ")";
    }
}
