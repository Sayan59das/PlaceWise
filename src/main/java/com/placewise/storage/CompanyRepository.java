package com.placewise.storage;

import com.placewise.model.Company;
import java.util.Optional;

/** Storage of companies (with their allowed branches), keyed by generated id. */
public interface CompanyRepository extends Repository<Company, Long> {

    /**
     * Finds a company by name, ignoring case.
     *
     * @param name the company name
     * @return the company, or empty
     */
    Optional<Company> findByName(String name);
}
