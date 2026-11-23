package com.placewise.report;

import java.util.List;

/** Renders a {@link Table} as aligned, human-readable plain text. */
public final class TextReportRenderer extends ReportRenderer {

    /** Creates the renderer (it has no settings). */
    public TextReportRenderer() {
        super();
    }

    @Override
    public String extension() {
        return ".txt";
    }

    @Override
    public String formatName() {
        return "text";
    }

    @Override
    public String render(Table table) {
        int[] widths = columnWidths(table);
        StringBuilder out = new StringBuilder();
        out.append(table.title()).append('\n');
        out.append("=".repeat(Math.max(table.title().length(), 1))).append("\n\n");
        appendRow(out, table.headers(), widths);
        out.append(ruleLine(widths)).append('\n');
        for (List<String> row : table.rows()) {
            appendRow(out, row, widths);
        }
        out.append('\n').append(table.rows().size()).append(table.rows().size() == 1 ? " row" : " rows").append('\n');
        return out.toString();
    }

    /** Every column is as wide as its longest cell, heading included. */
    private static int[] columnWidths(Table table) {
        int columns = table.headers().size();
        int[] widths = new int[columns];
        for (int c = 0; c < columns; c++) {
            widths[c] = table.headers().get(c).length();
        }
        for (List<String> row : table.rows()) {
            for (int c = 0; c < columns; c++) {
                widths[c] = Math.max(widths[c], row.get(c).length());
            }
        }
        return widths;
    }

    /** The line of dashes under the headings, for example {@code ----  -------}. */
    private static String ruleLine(int[] widths) {
        StringBuilder rule = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            if (c > 0) {
                rule.append("  ");
            }
            rule.append("-".repeat(widths[c]));
        }
        return rule.toString();
    }

    private static void appendRow(StringBuilder out, List<String> cells, int[] widths) {
        StringBuilder line = new StringBuilder();
        for (int c = 0; c < widths.length; c++) {
            if (c > 0) {
                line.append("  ");
            }
            line.append(String.format("%-" + widths[c] + "s", cells.get(c)));
        }
        out.append(line.toString().stripTrailing()).append('\n');
    }
}
