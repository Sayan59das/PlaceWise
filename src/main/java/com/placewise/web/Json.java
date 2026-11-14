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

    /** Pattern matching for switch picks the right branch from the value's type. */
    private static void append(StringBuilder out, Object value) {
        switch (value) {
            case null -> out.append("null");
            case String text -> appendText(out, text);
            case BigDecimal number -> out.append(number.toPlainString()); // 10.00, never 1.000E+1
            case Number number -> out.append(number);
            case Boolean flag -> out.append(flag);
            case Map<?, ?> map -> appendMap(out, map);
            case Collection<?> list -> appendList(out, list);
            default -> appendText(out, value.toString());
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
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c)); // any other control character
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        out.append('"');
    }
}
