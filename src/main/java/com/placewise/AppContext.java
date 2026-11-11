package com.placewise;

import com.placewise.analytics.WhatIfSimulator;
import com.placewise.analytics.WhyNotEligibleExplainer;
import com.placewise.audit.AuditBackup;
import com.placewise.audit.FileAuditLog;
import com.placewise.eligibility.EligibilityChecker;
import com.placewise.exception.PlaceWiseException;
import com.placewise.service.CompanyService;
import com.placewise.service.DriveService;
import com.placewise.service.EligibilityService;
import com.placewise.service.ImportService;
import com.placewise.service.ListingService;
import com.placewise.service.ReportService;
import com.placewise.service.StudentService;
import com.placewise.storage.ApplicationRepository;
import com.placewise.storage.CompanyRepository;
import com.placewise.storage.StudentRepository;
import com.placewise.storage.database.Database;
import com.placewise.storage.database.JdbcApplicationRepository;
import com.placewise.storage.database.JdbcCompanyRepository;
import com.placewise.storage.database.JdbcStudentRepository;
import com.placewise.storage.database.SchemaInitializer;
import com.placewise.storage.memory.InMemoryApplicationRepository;
import com.placewise.storage.memory.InMemoryCompanyRepository;
import com.placewise.storage.memory.InMemoryStudentRepository;
import java.nio.file.Path;

/**
 * The composition root: builds every repository and service once and hands them to the UI. With a
 * {@link Database} the repositories are JDBC ones; with {@code null} they are in-memory ones.
 * Closing the context is the program's graceful shutdown: audit log flushed and closed, backed up with a
 * byte-stream copy, database closed.
 */
public final class AppContext implements AutoCloseable {

    private final Database database;
    private final Path dataDir;
    private final FileAuditLog audit;
    private final EligibilityChecker checker = new EligibilityChecker();
    private final StudentService students;
    private final CompanyService companies;
    private final EligibilityService eligibility;
    private final ListingService listings;
    private final ReportService reports;
    private final ImportService imports;
    private final DriveService drives;

    private AppContext(Database database, Path dataDir) {
        this.database = database;
        this.dataDir = dataDir;
        this.audit = new FileAuditLog(dataDir.resolve("audit.log"));
        StudentRepository studentRepo;
        CompanyRepository companyRepo;
        ApplicationRepository applicationRepo;
        if (database == null) {
            studentRepo = new InMemoryStudentRepository();
            companyRepo = new InMemoryCompanyRepository();
            applicationRepo = new InMemoryApplicationRepository();
        } else {
            studentRepo = new JdbcStudentRepository(database);
            companyRepo = new JdbcCompanyRepository(database);
            applicationRepo = new JdbcApplicationRepository(database, studentRepo, companyRepo);
        }
        this.students = new StudentService(studentRepo);
        this.companies = new CompanyService(companyRepo);
        this.eligibility = new EligibilityService(students, companies, checker, audit);
        this.listings = new ListingService(students, companies, checker);
        this.reports = new ReportService(students, companies, listings, applicationRepo, checker);
        this.imports = new ImportService(studentRepo, companyRepo);
        this.drives = new DriveService(students, companies, applicationRepo, checker, audit);
    }

    /**
     * Builds the application.
     *
     * @param database the database to use (its schema is created if missing), or {@code null} for in-memory storage
     * @param dataDir  folder for the audit log, its backup, and default report/reject locations
     * @return the ready-to-use context
     */
    public static AppContext create(Database database, Path dataDir) {
        if (database != null) {
            SchemaInitializer.initialize(database);
        }
        return new AppContext(database, dataDir);
    }

    /**
     * Copies the audit log (byte by byte) to {@code audit-backup.log} next to it.
     *
     * @return the backup file
     */
    public Path backupAudit() {
        Path backup = dataDir.resolve("audit-backup.log");
        AuditBackup.copy(audit.file(), backup);
        return backup;
    }

    /**
     * Describes where data lives.
     *
     * @return for example {@code "in-memory (data is lost on exit)"}
     */
    public String storageDescription() {
        return database == null ? "in-memory (data is lost on exit)" : "database";
    }

    /**
     * Student use-cases.
     *
     * @return the service
     */
    public StudentService students() {
        return students;
    }

    /**
     * Company use-cases.
     *
     * @return the service
     */
    public CompanyService companies() {
        return companies;
    }

    /**
     * Eligibility questions.
     *
     * @return the service
     */
    public EligibilityService eligibility() {
        return eligibility;
    }

    /**
     * Eligible and ranked lists.
     *
     * @return the service
     */
    public ListingService listings() {
        return listings;
    }

    /**
     * Reports and exports.
     *
     * @return the service
     */
    public ReportService reports() {
        return reports;
    }

    /**
     * CSV imports.
     *
     * @return the service
     */
    public ImportService imports() {
        return imports;
    }

    /**
     * Placement drives.
     *
     * @return the service
     */
    public DriveService drives() {
        return drives;
    }

    /**
     * The shared eligibility rules.
     *
     * @return the checker
     */
    public EligibilityChecker checker() {
        return checker;
    }

    /**
     * Explains rejections.
     *
     * @return a new explainer over the shared rules
     */
    public WhyNotEligibleExplainer explainer() {
        return new WhyNotEligibleExplainer(checker);
    }

    /**
     * Runs what-if scenarios.
     *
     * @return a new simulator over the shared rules
     */
    public WhatIfSimulator whatIf() {
        return new WhatIfSimulator(checker);
    }

    /**
     * The folder for runtime files.
     *
     * @return the data directory
     */
    public Path dataDir() {
        return dataDir;
    }

    /**
     * Graceful shutdown: closes the audit log, backs it up, then closes the database. Every step runs even
     * if an earlier one fails, so a full disk cannot leave the database handle open.
     *
     * @throws PlaceWiseException the first failure (later ones are attached as suppressed exceptions)
     */
    @Override
    public void close() {
        PlaceWiseException failure = null;
        try {
            audit.close();
        } catch (PlaceWiseException e) {
            failure = e;
        }
        try {
            backupAudit();
        } catch (PlaceWiseException e) {
            failure = remember(failure, e);
        }
        if (database != null) {
            database.close();
        }
        if (failure != null) {
            throw failure;
        }
    }

    private static PlaceWiseException remember(PlaceWiseException first, PlaceWiseException later) {
        if (first == null) {
            return later;
        }
        first.addSuppressed(later);
        return first;
    }
}
