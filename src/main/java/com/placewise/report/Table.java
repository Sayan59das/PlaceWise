package com.placewise.report;

import java.util.List;

/**
 * A report as plain data: a title, column headings and rows of text. Renderers turn it into a file
 * format; keeping the data separate from the format is what lets one report be written as text <em>and</em> CSV.
 *
 * @param title   what the report is about
 * @param headers column headings
 * @param rows    one list of cell texts per row, each as long as {@code headers}
 */
public record Table(String title, List<String> headers, List<List<String>> rows) {
}
