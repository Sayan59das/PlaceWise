package com.placewise.storage.memory;

import com.placewise.exception.InvalidCompanyException;
import com.placewise.exception.PlaceWiseException;
import com.placewise.model.Company;
import com.placewise.storage.CompanyRepository;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Companies kept in insertion order with ids handed out by an {@link AtomicLong}, like AUTO_INCREMENT. */
public final class InMemoryCompanyRepository extends InMemoryRepository<Company, Long>
        implements CompanyRepository {

    private final AtomicLong nextId = new AtomicLong();

    /** Creates an empty repository. */
    public InMemoryCompanyRepository() {
        super(new LinkedHashMap<>());
    }

    @Override
    protected Long idOf(Company company) {
        return company.id();
    }

    @Override
    protected PlaceWiseException duplicate(Company company) {
        return new InvalidCompanyException("Company name already exists: " + company.name());
    }

    @Override
    public synchronized Company save(Company company) {
        if (nameTakenByAnother(company.name(), -1)) {
            throw duplicate(company);
        }
        Company saved = company.withId(nextId.incrementAndGet());
        store().put(saved.id(), saved);
        return saved;
    }

    @Override
    public synchronized boolean update(Company company) {
        if (!store().containsKey(company.id())) {
            return false;
        }
        if (nameTakenByAnother(company.name(), company.id())) {
            throw duplicate(company);
        }
        store().put(company.id(), company);
        return true;
    }

    @Override
    public synchronized Optional<Company> findByName(String name) {
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        for (Company company : store().values()) {
            if (company.name().toLowerCase(Locale.ROOT).equals(wanted)) {
                return Optional.of(company);
            }
        }
        return Optional.empty();
    }

    private boolean nameTakenByAnother(String name, long ownId) {
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        for (Company company : store().values()) {
            if (company.id() != ownId && company.name().toLowerCase(Locale.ROOT).equals(wanted)) {
                return true;
            }
        }
        return false;
    }
}
