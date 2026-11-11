package com.placewise.storage.memory;

import com.placewise.exception.PersistenceException;
import com.placewise.exception.PlaceWiseException;
import com.placewise.model.Application;
import com.placewise.model.ApplicationKey;
import com.placewise.storage.ApplicationRepository;
import java.util.LinkedHashMap;
import java.util.List;

/** Applications kept in insertion order. Unlike the SQL tables it does not check foreign keys. */
public final class InMemoryApplicationRepository extends InMemoryRepository<Application, ApplicationKey>
        implements ApplicationRepository {

    /** Creates an empty repository. */
    public InMemoryApplicationRepository() {
        super(new LinkedHashMap<>());
    }

    @Override
    protected ApplicationKey idOf(Application application) {
        return application.key();
    }

    @Override
    protected PlaceWiseException duplicate(Application application) {
        return new PersistenceException("Application already exists: " + application);
    }

    @Override
    public synchronized void replaceAll(List<Application> applications) {
        store().clear();
        for (Application application : applications) {
            store().put(application.key(), application);
        }
    }
}
