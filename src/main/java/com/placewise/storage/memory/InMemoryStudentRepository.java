package com.placewise.storage.memory;

import com.placewise.exception.DuplicateRollNoException;
import com.placewise.exception.PlaceWiseException;
import com.placewise.model.Student;
import com.placewise.storage.StudentRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/** Students kept in a {@link TreeMap}, so {@code findAll()} is sorted by roll number like the SQL version. */
public final class InMemoryStudentRepository extends InMemoryRepository<Student, String>
        implements StudentRepository {

    /** Creates an empty repository. */
    public InMemoryStudentRepository() {
        super(new TreeMap<>());
    }

    @Override
    protected String idOf(Student student) {
        return student.rollNo();
    }

    @Override
    protected PlaceWiseException duplicate(Student student) {
        return new DuplicateRollNoException(student.rollNo());
    }

    @Override
    public synchronized void saveAll(List<Student> students) {
        // Check everything first, then store: that is the in-memory version of "commit or roll back".
        Set<String> seen = new HashSet<>();
        for (Student student : students) {
            if (!seen.add(student.rollNo()) || store().containsKey(student.rollNo())) {
                throw new DuplicateRollNoException(student.rollNo());
            }
        }
        for (Student student : students) {
            store().put(student.rollNo(), student);
        }
    }
}
