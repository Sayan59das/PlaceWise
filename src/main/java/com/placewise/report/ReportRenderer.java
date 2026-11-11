package com.placewise.report;

/**
 * Turns a {@link Table} into text in one file format. An <em>abstract class</em> because every format
 * shares the same two questions (what is your file extension? how do you render?) and exporters only
 * need to know this type: the concrete subclass is chosen at run time (polymorphism).
 */
public abstract class ReportRenderer {

    /** For subclasses only. */
    protected ReportRenderer() {
        // nothing to initialise
    }

    /**
     * Returns the file extension including the dot.
     *
     * @return for example {@code ".txt"}
     */
    public abstract String extension();

    /**
     * Returns a short name for menus.
     *
     * @return for example {@code "text"}
     */
    public abstract String formatName();

    /**
     * Renders the table.
     *
     * @param table the report data
     * @return the complete file content, ending with a line break
     */
    public abstract String render(Table table);
}
