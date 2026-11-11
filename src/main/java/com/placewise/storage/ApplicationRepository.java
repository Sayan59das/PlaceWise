package com.placewise.storage;

import com.placewise.model.Application;
import com.placewise.model.ApplicationKey;
import java.util.List;

/** Storage of applications, keyed by (roll number, company id). */
public interface ApplicationRepository extends Repository<Application, ApplicationKey> {

    /**
     * Replaces the whole table content with the results of the latest drive, atomically.
     *
     * @param applications the new content
     */
    void replaceAll(List<Application> applications);
}
