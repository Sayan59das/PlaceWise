package com.placewise.web;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

/**
 * Turns plain Java values into JSON text, the format the web page reads.
 *
 * <p>Only six kinds of value are needed: text, numbers, true/false, null, lists and maps. Anything else
 * (an enum, a file path) is written as its {@code toString()} text. Writing this by hand keeps the project
 * free of extra libraries, and it is short enough to read in one sitting.
 */
public final class Json {

    private Json() {
    }

    /**
     * Converts a value to JSON.
     *
     * @param value a String, Number, Boolean, {@code null}, Map, Collection, or anything with a useful toString()
     * @return the JSON text, for example {@code {"rollNo":"CU2501","cgpa":9.42}}
     */
    public static String write(Object value) {
        StringBuilder out = new StringBuilder();
        append(out, value);
        return out.toString();
    }

    /** Looks at the type of the value and writes it the matching way. */
    private static void append(StringBuilder out, Object value) {
        if (value == null) {
            out.append("null");
        } else if (value instanceof String text) {
            appendText(out, text);
        } else if (value instanceof BigDecimal decimal) {
            out.append(decimal.toPlainString()); // 10.00, never 1.000E+1
        } else if (value instanceof Number number) {
            out.append(number);
        } else if (value instanceof Boolean flag) {
            out.append(flag);
        } else if (value instanceof Map<?, ?> map) {
            appendMap(out, map);
        } else if (value instanceof Collection<?> list) {
            appendList(out, list);
        } else {
            appendText(out, value.toString());
        }
    }

    private static void appendMap(StringBuilder out, Map<?, ?> map) {
        out.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!first) {
                out.append(',');
            }
            first = false;
            appendText(out, String.valueOf(entry.getKey()));
            out.append(':');
            append(out, entry.getValue());
        }
        out.append('}');
    }

    private static void appendList(StringBuilder out, Collection<?> list) {
        out.append('[');
        boolean first = true;
        for (Object item : list) {
            if (!first) {
                out.append(',');
            }
            first = false;
            append(out, item);
        }
        out.append(']');
    }

    /** Writes text in double quotes, escaping the characters JSON does not allow inside a string. */
    private static void appendText(StringBuilder out, String text) {
        out.append('"');
        for (char c : text.toCharArray()) {
            if (c == '"') {
                out.append("\\\"");
            } else if (c == '\\') {
                out.append("\\\\");
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c)); // any other control character
            } else {
                out.append(c);
            }
        }
        out.append('"');
    }
}
