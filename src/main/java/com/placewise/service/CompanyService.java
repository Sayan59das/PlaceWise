package com.placewise.service;

import com.placewise.exception.CompanyNotFoundException;
import com.placewise.model.Company;
import com.placewise.storage.CompanyRepository;
import com.placewise.validation.CompanyValidator;
import java.util.List;
import java.util.Optional;

/** Company use-cases: validation first, then the repository. */
public final class CompanyService {

    private final CompanyRepository repository;

    /**
     * Creates the service.
     *
     * @param repository where companies are stored
     */
    public CompanyService(CompanyRepository repository) {
        this.repository = repository;
    }

    /**
     * Adds a company.
     *
     * @param company the company (its id is ignored)
     * @return the stored company with its new id
     * @throws com.placewise.exception.InvalidCompanyException if a rule is broken or the name is taken
     */
    public Company add(Company company) {
        return repository.save(CompanyValidator.requireValid(company));
    }

    /**
     * Replaces a company's criteria.
     *
     * @param company the new data, carrying the id of the company to change
     * @return the stored company
     * @throws CompanyNotFoundException if the id does not exist
     */
    public Company update(Company company) {
        CompanyValidator.requireValid(company);
        if (!repository.update(company)) {
            throw new CompanyNotFoundException(company.id());
        }
        return company;
    }

    /**
     * Deletes a company.
     *
     * @param id the company id
     * @throws CompanyNotFoundException if the id does not exist
     */
    public void delete(long id) {
        if (!repository.delete(id)) {
            throw new CompanyNotFoundException(id);
        }
    }

    /**
     * Looks a company up or fails.
     *
     * @param id the company id
     * @return the company
     * @throws CompanyNotFoundException if the id does not exist
     */
    public Company get(long id) {
        Optional<Company> found = repository.findById(id);
        if (found.isEmpty()) {
            throw new CompanyNotFoundException(id);
        }
        return found.get();
    }

    /**
     * Looks a company up by name.
     *
     * @param name the name, any case
     * @return the company, or empty
     */
    public Optional<Company> findByName(String name) {
        return repository.findByName(name);
    }

    /**
     * Lists every company ordered by id.
     *
     * @return all companies
     */
    public List<Company> listAll() {
        return repository.findAll();
    }
}
