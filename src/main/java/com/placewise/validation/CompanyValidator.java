package com.placewise.validation;

import com.placewise.exception.InvalidCompanyException;
import com.placewise.model.Cgpa;
import com.placewise.model.Company;
import java.util.ArrayList;
import java.util.List;

/** Checks a {@link Company} and reports every problem at once. */
public final class CompanyValidator {

    private CompanyValidator() {
    }

    /** Returns every rule the company breaks; an empty list means the company is valid. */
    public static List<String> problems(Company company) {
        List<String> problems = new ArrayList<>();
        if (company.name().isBlank()) {
            problems.add("Company name must not be blank");
        }
        if (!Range.isBetween(company.minCgpa(), Cgpa.MIN, Cgpa.MAX)) {
            problems.add("Minimum CGPA must be between 0.00 and 10.00");
        }
        if (company.maxBacklogs() < 0) {
            problems.add("Maximum backlogs must not be negative");
        }
        if (company.seats() <= 0) {
            problems.add("Seats must be greater than zero");
        }
        if (company.packageLpa().signum() < 0) {
            problems.add("Package must not be negative");
        }
        return problems;
    }

    /** Throws {@link InvalidCompanyException} (listing all problems) if the company is not valid. */
    public static Company requireValid(Company company) {
        List<String> problems = problems(company);
        if (!problems.isEmpty()) {
            throw new InvalidCompanyException(problems);
        }
        return company;
    }
}
