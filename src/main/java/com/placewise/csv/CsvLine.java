package com.placewise.csv;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes one CSV line (RFC 4180 style): fields separated by commas, a field containing a
 * comma or a quote is wrapped in double quotes, and a quote inside quotes is doubled ({@code ""}).
 * A quoted field may not contain a line break; the importer reads one physical line per record.
 */
public final class CsvLine {

    private CsvLine() {
    }

    /**
     * Splits a line into fields.
     *
     * @param line one line of CSV text
     * @return the fields, never empty (an empty line gives one empty field)
     * @throws IllegalArgumentException if a quote is opened and never closed
     */
    public static List<String> parse(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean atFieldStart = true;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (inQuotes) {
                if (ch != '"') {
                    current.append(ch);
                } else if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"'); // doubled quote = one literal quote
                    i++;
                } else {
                    inQuotes = false;
                }
            } else if (ch == '"' && atFieldStart) {
                inQuotes = true;
                atFieldStart = false;
            } else if (ch == ',') {
                fields.add(current.toString());
                current.setLength(0);
                atFieldStart = true;
            } else {
                current.append(ch);
                atFieldStart = false;
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("a quoted value is not closed");
        }
        fields.add(current.toString());
        return fields;
    }

    /**
     * Joins fields into one line, quoting where needed.
     *
     * @param fields the values
     * @return the CSV line without a line terminator
     */
    public static String format(List<String> fields) {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(escape(fields.get(i)));
        }
        return line.toString();
    }

    /**
     * Quotes a single value if it contains a comma, quote, line break, or leading/trailing space.
     *
     * @param value the raw value ({@code null} is written as empty)
     * @return the value as it should appear in a CSV file
     */
    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        boolean needsQuotes = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
                || !value.equals(value.strip());
        return needsQuotes ? '"' + value.replace("\"", "\"\"") + '"' : value;
    }
}
