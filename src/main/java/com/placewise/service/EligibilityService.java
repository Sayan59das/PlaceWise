package com.placewise.service;

import com.placewise.audit.AuditEvent;
import com.placewise.audit.AuditLog;
import com.placewise.eligibility.EligibilityChecker;
import com.placewise.eligibility.EligibilityResult;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.StudentSort;
import java.util.List;

/** The three eligibility questions the menu asks, each recorded in the audit log. */
public final class EligibilityService {

    private final StudentService students;
    private final CompanyService companies;
    private final EligibilityChecker checker;
    private final AuditLog audit;

    /**
     * Creates the service.
     *
     * @param students  student lookups
     * @param companies company lookups
     * @param checker   the eligibility rules
     * @param audit     where ELIGIBILITY_CHECK events are written
     */
    public EligibilityService(StudentService students, CompanyService companies, EligibilityChecker checker,
                              AuditLog audit) {
        this.students = students;
        this.companies = companies;
        this.checker = checker;
        this.audit = audit;
    }

    /**
     * One student against one company.
     *
     * @param rollNo    the student's roll number
     * @param companyId the company id
     * @return the verdict
     * @throws com.placewise.exception.StudentNotFoundException if the student does not exist
     * @throws com.placewise.exception.CompanyNotFoundException if the company does not exist
     */
    public EligibilityResult check(String rollNo, long companyId) {
        Student student = students.get(rollNo);
        Company company = companies.get(companyId);
        EligibilityResult result = checker.check(student, company);
        record(result);
        return result;
    }

    /**
     * One student against every company.
     *
     * @param rollNo the student's roll number
     * @return one verdict per company, ordered by company id
     */
    public List<EligibilityResult> checkAgainstAllCompanies(String rollNo) {
        Student student = students.get(rollNo);
        List<EligibilityResult> results = checker.checkAgainstAll(student, companies.listAll());
        for (EligibilityResult result : results) {
            record(result);
        }
        return results;
    }

    /**
     * Every student against one company.
     *
     * @param companyId the company id
     * @return one verdict per student, ordered by roll number
     */
    public List<EligibilityResult> checkAllStudents(long companyId) {
        Company company = companies.get(companyId);
        List<EligibilityResult> results = checker.checkAllStudents(company, students.listAll(StudentSort.ROLL_NO));
        for (EligibilityResult result : results) {
            record(result);
        }
        return results;
    }

    private void record(EligibilityResult result) {
        audit.log(AuditEvent.ELIGIBILITY_CHECK, result.student().rollNo() + " vs " + result.company().name()
                + " -> " + (result.eligible() ? "ELIGIBLE" : "NOT ELIGIBLE"));
    }
}
