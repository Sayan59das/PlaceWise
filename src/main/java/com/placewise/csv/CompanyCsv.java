package com.placewise.csv;

import com.placewise.model.Company;
import com.placewise.validation.CompanyValidator;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reads and writes company files: {@code name,minCgpa,maxBacklogs,allowedBranches,packageLpa,seats}.
 * {@code allowedBranches} lists branches separated by {@code |}; an empty cell means every branch.
 */
public final class CompanyCsv extends CsvImporter<Company> {

    /** The header every company file must start with. */
    public static final List<String> HEADER =
            List.of("name", "minCgpa", "maxBacklogs", "allowedBranches", "packageLpa", "seats");

    private CompanyCsv() {
        super(HEADER);
    }

    /** Creates a reader for company files. */
    public static CompanyCsv importer() {
        return new CompanyCsv();
    }

    /** Converts one row into a validated company with id 0 (the repository gives the real id). */
    @Override
    protected Company parseRow(List<String> fields) {
        if (fields.size() != HEADER.size()) {
            throw new IllegalArgumentException("expected " + HEADER.size() + " columns but found " + fields.size());
        }
        Set<String> branches = new HashSet<>();
        if (!fields.get(3).isBlank()) {
            for (String branch : fields.get(3).split("\\|")) {
                branches.add(branch);
            }
        }
        Company company = new Company(0, fields.get(0), StudentCsv.parseCgpa(fields.get(1)),
                StudentCsv.parseInt(fields.get(2), "maxBacklogs"), branches, parseMoney(fields.get(4)),
                StudentCsv.parseInt(fields.get(5), "seats"));
        return CompanyValidator.requireValid(company);
    }

    /** Two rows with the same company name (ignoring case) are duplicates. */
    @Override
    protected String keyOf(Company company) {
        return company.name().toLowerCase(Locale.ROOT);
    }

    private static BigDecimal parseMoney(String text) {
        BigDecimal value;
        try {
            value = new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("package '" + text.trim() + "' is not a number");
        }
        if (value.precision() - value.scale() > 8 || value.scale() > 20) {
            throw new IllegalArgumentException("package '" + text.trim() + "' is out of range");
        }
        return value;
    }

    /** Turns a company into CSV fields, in header order (the id is not written). */
    public static List<String> toFields(Company company) {
        return List.of(company.name(), company.minCgpa().toPlainString(), String.valueOf(company.maxBacklogs()),
                String.join("|", company.allowedBranches()), company.packageLpa().toPlainString(),
                String.valueOf(company.seats()));
    }

    /** Writes companies to a CSV file that {@link #importer()} can read back. */
    public static void export(Path file, Collection<Company> companies) {
        List<List<String>> rows = new ArrayList<>();
        for (Company company : companies) {
            rows.add(toFields(company));
        }
        CsvFiles.write(file, HEADER, rows);
    }
}
