package com.placewise.storage;

import com.placewise.model.Student;
import java.util.List;

/** Storage of students, keyed by (normalized) roll number. */
public interface StudentRepository extends Repository<Student, String> {

    /**
     * Inserts many students <em>atomically</em>: either all are stored or none are.
     *
     * @param students the students to insert
     * @throws com.placewise.exception.DuplicateRollNoException if a roll number repeats in the list or already exists
     * @throws com.placewise.exception.PersistenceException     if the database rejects a row (nothing is stored)
     */
    void saveAll(List<Student> students);
}
