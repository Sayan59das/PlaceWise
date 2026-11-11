package com.placewise.service;

import com.placewise.csv.CompanyCsv;
import com.placewise.csv.CsvFiles;
import com.placewise.csv.ImportResult;
import com.placewise.csv.RejectedRow;
import com.placewise.csv.StudentCsv;
import com.placewise.exception.ImportException;
import com.placewise.exception.PlaceWiseException;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.storage.CompanyRepository;
import com.placewise.storage.StudentRepository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CSV import use-cases. Rows that are bad - or that clash with data already stored - are written to a
 * {@code rejects.csv}; the rest is stored. A student import is one all-or-nothing transaction.
 */
public final class ImportService {

    private final StudentRepository students;
    private final CompanyRepository companies;

    /**
     * Creates the service.
     *
     * @param students  student storage
     * @param companies company storage
     */
    public ImportService(StudentRepository students, CompanyRepository companies) {
        this.students = students;
        this.companies = companies;
    }

    /**
     * Imports students from a CSV file.
     *
     * @param file        the CSV file
     * @param rejectsFile where to write refused rows (only written if there are any)
     * @return what happened
     * @throws ImportException if the file is unusable or the database refused the batch (nothing stored then)
     */
    public ImportSummary importStudents(Path file, Path rejectsFile) {
        ImportResult<Student> result = StudentCsv.importer().read(file);
        List<RejectedRow> rejected = new ArrayList<>(result.rejected());
        Set<String> existing = new HashSet<>();
        for (Student student : students.findAll()) {
            existing.add(student.rollNo());
        }
        List<Student> toStore = new ArrayList<>();
        for (ImportResult.Row<Student> row : result.accepted()) {
            if (existing.contains(row.item().rollNo())) {
                rejected.add(new RejectedRow(row.lineNumber(), row.rawLine(),
                        "roll number " + row.item().rollNo() + " already exists"));
            } else {
                toStore.add(row.item());
            }
        }
        try {
            students.saveAll(toStore);
        } catch (PlaceWiseException e) {
            throw new ImportException("Nothing was imported: " + e.getMessage(), e);
        }
        return finish(result.dataRows(), toStore.size(), rejected, rejectsFile);
    }

    /**
     * Imports companies from a CSV file. Each company is stored on its own, so one clash with an
     * existing name rejects only that row.
     *
     * @param file        the CSV file
     * @param rejectsFile where to write refused rows (only written if there are any)
     * @return what happened
     * @throws ImportException if the file is unusable
     */
    public ImportSummary importCompanies(Path file, Path rejectsFile) {
        ImportResult<Company> result = CompanyCsv.importer().read(file);
        List<RejectedRow> rejected = new ArrayList<>(result.rejected());
        int stored = 0;
        for (ImportResult.Row<Company> row : result.accepted()) {
            try {
                companies.save(row.item());
                stored++;
            } catch (PlaceWiseException e) {
                rejected.add(new RejectedRow(row.lineNumber(), row.rawLine(), e.getMessage()));
            }
        }
        return finish(result.dataRows(), stored, rejected, rejectsFile);
    }

    private static ImportSummary finish(int dataRows, int imported, List<RejectedRow> rejected, Path rejectsFile) {
        if (rejected.isEmpty()) {
            return new ImportSummary(dataRows, imported, 0, null);
        }
        // Parse-time and database-clash rejections are found in different passes; present them in file order.
        rejected.sort((a, b) -> Integer.compare(a.lineNumber(), b.lineNumber()));
        CsvFiles.writeRejects(rejectsFile, rejected);
        return new ImportSummary(dataRows, imported, rejected.size(), rejectsFile);
    }
}
