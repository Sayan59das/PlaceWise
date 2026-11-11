package com.placewise.service;

import com.placewise.audit.AuditLog;
import com.placewise.eligibility.EligibilityChecker;
import com.placewise.storage.memory.InMemoryApplicationRepository;
import com.placewise.storage.memory.InMemoryCompanyRepository;
import com.placewise.storage.memory.InMemoryStudentRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Wires the services over in-memory repositories, with an audit log that remembers its lines. */
final class ServiceFixture {

    final List<String> auditLines = Collections.synchronizedList(new ArrayList<>());
    final AuditLog audit = (event, message) -> auditLines.add(event + ": " + message);
    final InMemoryStudentRepository studentRepo = new InMemoryStudentRepository();
    final InMemoryCompanyRepository companyRepo = new InMemoryCompanyRepository();
    final InMemoryApplicationRepository applicationRepo = new InMemoryApplicationRepository();
    final EligibilityChecker checker = new EligibilityChecker();
    final StudentService students = new StudentService(studentRepo);
    final CompanyService companies = new CompanyService(companyRepo);
    final EligibilityService eligibility = new EligibilityService(students, companies, checker, audit);
    final ListingService listings = new ListingService(students, companies, checker);
    final ReportService reports = new ReportService(students, companies, listings, applicationRepo, checker);
    final ImportService imports = new ImportService(studentRepo, companyRepo);
}
