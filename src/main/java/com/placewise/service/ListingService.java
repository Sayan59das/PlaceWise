package com.placewise.service;

import com.placewise.eligibility.EligibilityChecker;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.ranking.RankedStudent;
import com.placewise.ranking.StudentRanking;
import com.placewise.ranking.StudentSort;
import java.util.List;

/** Eligible and ranked student lists for a company, with optional filters. */
public final class ListingService {

    private final StudentService students;
    private final CompanyService companies;
    private final EligibilityChecker checker;

    /**
     * Creates the service.
     *
     * @param students  student lookups
     * @param companies company lookups
     * @param checker   the eligibility rules
     */
    public ListingService(StudentService students, CompanyService companies, EligibilityChecker checker) {
        this.students = students;
        this.companies = companies;
        this.checker = checker;
    }

    /**
     * Students eligible for a company, narrowed by a filter, ordered by roll number.
     *
     * @param companyId the company id
     * @param filter    branch / minimum-CGPA restrictions
     * @return the matching students
     * @throws com.placewise.exception.CompanyNotFoundException if the company does not exist
     */
    public List<Student> eligibleStudents(long companyId, EligibleFilter filter) {
        Company company = companies.get(companyId);
        // A stream reads like the sentence "keep the eligible students that also pass the filter".
        return students.listAll(StudentSort.ROLL_NO).stream()
                .filter(student -> checker.isEligible(student, company))
                .filter(student -> filter.matches(student))
                .toList();
    }

    /**
     * The same students, ranked.
     *
     * @param companyId the company id
     * @param filter    restrictions
     * @return ranked students, best first
     */
    public List<RankedStudent> ranked(long companyId, EligibleFilter filter) {
        return StudentRanking.rank(eligibleStudents(companyId, filter));
    }

    /**
     * The best {@code n} eligible students.
     *
     * @param companyId the company id
     * @param filter    restrictions
     * @param n         how many to return
     * @return up to {@code n} ranked students
     */
    public List<RankedStudent> top(long companyId, EligibleFilter filter, int n) {
        return StudentRanking.top(eligibleStudents(companyId, filter), n);
    }

    /**
     * One page of the ranked list.
     *
     * @param companyId the company id
     * @param filter    restrictions
     * @param page      1-based page number
     * @param pageSize  students per page
     * @return the students on that page
     */
    public List<RankedStudent> page(long companyId, EligibleFilter filter, int page, int pageSize) {
        return StudentRanking.page(eligibleStudents(companyId, filter), page, pageSize);
    }
}
