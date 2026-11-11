package com.placewise.service;

import com.placewise.exception.StudentNotFoundException;
import com.placewise.model.Student;
import com.placewise.ranking.StudentSort;
import com.placewise.storage.StudentRepository;
import com.placewise.validation.StudentValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Student use-cases: validation first, then the repository. */
public final class StudentService {

    private final StudentRepository repository;

    /**
     * Creates the service.
     *
     * @param repository where students are stored
     */
    public StudentService(StudentRepository repository) {
        this.repository = repository;
    }

    /**
     * Adds a new student.
     *
     * @param student the student
     * @return the stored student
     * @throws com.placewise.exception.InvalidStudentException if a rule is broken
     * @throws com.placewise.exception.DuplicateRollNoException if the roll number exists
     */
    public Student add(Student student) {
        return repository.save(StudentValidator.requireValid(student));
    }

    /**
     * Replaces an existing student's data.
     *
     * @param student the new data, with the roll number of the student to change
     * @return the stored student
     * @throws com.placewise.exception.InvalidStudentException if a rule is broken
     * @throws StudentNotFoundException                      if the roll number does not exist
     */
    public Student update(Student student) {
        StudentValidator.requireValid(student);
        if (!repository.update(student)) {
            throw new StudentNotFoundException(student.rollNo());
        }
        return student;
    }

    /**
     * Deletes a student (and, through the database cascade, their applications).
     *
     * @param rollNo the roll number, any case
     * @throws StudentNotFoundException if there is no such student
     */
    public void delete(String rollNo) {
        String key = Student.normalizeRoll(rollNo);
        if (!repository.delete(key)) {
            throw new StudentNotFoundException(key);
        }
    }

    /**
     * Looks a student up.
     *
     * @param rollNo the roll number, any case
     * @return the student, or empty
     */
    public Optional<Student> find(String rollNo) {
        return repository.findById(Student.normalizeRoll(rollNo));
    }

    /**
     * Looks a student up or fails.
     *
     * @param rollNo the roll number, any case
     * @return the student
     * @throws StudentNotFoundException if there is no such student
     */
    public Student get(String rollNo) {
        Optional<Student> found = find(rollNo);
        if (found.isEmpty()) {
            throw new StudentNotFoundException(Student.normalizeRoll(rollNo));
        }
        return found.get();
    }

    /**
     * Finds students whose name contains a fragment, ignoring case.
     *
     * @param fragment part of a name; blank matches everyone
     * @return matching students ordered by name
     */
    public List<Student> searchByName(String fragment) {
        String needle = fragment.trim().toLowerCase(Locale.ROOT);
        List<Student> matches = new ArrayList<>();
        for (Student student : repository.findAll()) {
            if (student.name().toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(student);
            }
        }
        matches.sort(StudentSort.NAME.comparator());
        return matches;
    }

    /**
     * Lists every student in a chosen order.
     *
     * @param sort the sort field
     * @return all students sorted
     */
    public List<Student> listAll(StudentSort sort) {
        List<Student> all = new ArrayList<>(repository.findAll());
        all.sort(sort.comparator());
        return all;
    }
}
