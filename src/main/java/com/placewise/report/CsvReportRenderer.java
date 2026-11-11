package com.placewise.report;

import com.placewise.csv.CsvLine;
import java.util.List;

/** Renders a {@link Table} as CSV (header line, then one line per row; the title is not part of the file). */
public final class CsvReportRenderer extends ReportRenderer {

    /** Creates the renderer (it has no settings). */
    public CsvReportRenderer() {
        super();
    }

    @Override
    public String extension() {
        return ".csv";
    }

    @Override
    public String formatName() {
        return "csv";
    }

    @Override
    public String render(Table table) {
        StringBuilder out = new StringBuilder();
        out.append(CsvLine.format(table.headers())).append('\n');
        for (List<String> row : table.rows()) {
            out.append(CsvLine.format(row)).append('\n');
        }
        return out.toString();
    }
}
