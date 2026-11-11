package com.placewise.service;

import com.placewise.audit.AuditLog;
import com.placewise.drive.DriveConfig;
import com.placewise.drive.DriveDataGenerator;
import com.placewise.drive.DriveInvariants;
import com.placewise.drive.DriveResult;
import com.placewise.drive.PlacementDrive;
import com.placewise.eligibility.EligibilityChecker;
import com.placewise.exception.DriveException;
import com.placewise.exception.PlaceWiseException;
import com.placewise.model.Application;
import com.placewise.model.ApplicationKey;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.StudentSort;
import com.placewise.storage.ApplicationRepository;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs placement drives on generated or stored data and checks the invariants afterwards. */
public final class DriveService {

    private final StudentService students;
    private final CompanyService companies;
    private final ApplicationRepository applications;
    private final EligibilityChecker checker;
    private final AuditLog audit;

    /**
     * Creates the service.
     *
     * @param students     stored students
     * @param companies    stored companies
     * @param applications where drive results are saved
     * @param checker      the eligibility rules
     * @param audit        the audit log the drive writes to
     */
    public DriveService(StudentService students, CompanyService companies, ApplicationRepository applications,
                        EligibilityChecker checker, AuditLog audit) {
        this.students = students;
        this.companies = companies;
        this.applications = applications;
        this.checker = checker;
        this.audit = audit;
    }

    /**
     * Runs a drive on freshly generated data (nothing is stored).
     *
     * @param config   sizes, threads, seed
     * @param progress where the progress monitor prints, or {@code null}
     * @return the result and the invariant verdict
     */
    public DriveOutcome runGenerated(DriveConfig config, PrintStream progress) {
        List<Student> generatedStudents = DriveDataGenerator.students(config.students(), config.seed());
        List<Company> generatedCompanies = DriveDataGenerator.companies(config.companies(),
                config.seatsPerCompany(), config.seed());
        return execute(generatedStudents, generatedCompanies, config, progress, false);
    }

    /**
     * Runs a drive on the students and companies currently stored; the outcome replaces the stored applications.
     *
     * @param config   threads and seed (sizes are ignored: the stored data decides)
     * @param progress where the progress monitor prints, or {@code null}
     * @return the result and the invariant verdict
     * @throws DriveException if there are no students or no companies
     */
    public DriveOutcome runStored(DriveConfig config, PrintStream progress) {
        List<Student> storedStudents = students.listAll(StudentSort.ROLL_NO);
        List<Company> storedCompanies = companies.listAll();
        if (storedStudents.isEmpty() || storedCompanies.isEmpty()) {
            throw new DriveException("Add at least one student and one company first (or import the sample data)");
        }
        return execute(storedStudents, storedCompanies, config, progress, true);
    }

    private DriveOutcome execute(List<Student> driveStudents, List<Company> driveCompanies, DriveConfig config,
                                 PrintStream progress, boolean save) {
        DriveResult result = new PlacementDrive(driveStudents, driveCompanies, config, checker, audit, progress).run();
        List<String> violations = DriveInvariants.check(result, checker);
        String persistence = "Generated data: results are shown but not saved";
        if (save) {
            try {
                applications.replaceAll(onePerPair(result.applications()));
                persistence = "Results saved (the stored applications were replaced)";
            } catch (PlaceWiseException e) {
                persistence = "Results could NOT be saved: " + e.getMessage();
            }
        }
        return new DriveOutcome(result, violations, persistence);
    }

    /** The table allows one row per (student, company); a rejected duplicate gives way to the live application. */
    private static List<Application> onePerPair(List<Application> all) {
        Map<ApplicationKey, Application> chosen = new LinkedHashMap<>();
        for (Application application : all) {
            Application old = chosen.get(application.key());
            if (old == null || old.status() == ApplicationStatus.REJECTED) {
                chosen.put(application.key(), application); // keep the live application, not the rejected duplicate
            }
        }
        return List.copyOf(chosen.values());
    }
}
