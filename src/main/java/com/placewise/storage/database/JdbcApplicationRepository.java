package com.placewise.storage.database;

import com.placewise.exception.PersistenceException;
import com.placewise.model.Application;
import com.placewise.model.ApplicationKey;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.storage.ApplicationRepository;
import com.placewise.storage.CompanyRepository;
import com.placewise.storage.StudentRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stores applications using plain JDBC. The table only keeps the roll number, the company id and the
 * status, so the full student and company are looked up through their own repositories when reading.
 */
public final class JdbcApplicationRepository extends JdbcRepositoryBase implements ApplicationRepository {

    private static final String INSERT = "INSERT INTO applications (roll_no, company_id, status) VALUES (?, ?, ?)";

    private final StudentRepository students;
    private final CompanyRepository companies;

    /** Creates the repository; the other two repositories are used to rebuild full objects. */
    public JdbcApplicationRepository(ConnectionProvider connections, StudentRepository students,
                                     CompanyRepository companies) {
        super(connections);
        this.students = students;
        this.companies = companies;
    }

    @Override
    public Application save(Application application) {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {
            fillInsert(statement, application);
            statement.executeUpdate();
            return application;
        } catch (SQLException e) {
            if (isDuplicateKey(e)) {
                throw new PersistenceException("Application already exists: " + application, e);
            }
            throw failure("Could not save application " + application, e);
        }
    }

    @Override
    public Optional<Application> findById(ApplicationKey key) {
        String status = null;
        String sql = "SELECT status FROM applications WHERE roll_no = ? AND company_id = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.rollNo());
            statement.setLong(2, key.companyId());
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    status = rs.getString("status");
                }
            }
        } catch (SQLException e) {
            throw failure("Could not read application " + key, e);
        }
        if (status == null) {
            return Optional.empty();
        }
        Optional<Student> student = students.findById(key.rollNo());
        Optional<Company> company = companies.findById(key.companyId());
        if (student.isEmpty() || company.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new Application(student.get(), company.get(), ApplicationStatus.valueOf(status)));
    }

    @Override
    public List<Application> findAll() {
        // Load every student and company once, so each row can be turned into objects quickly.
        Map<String, Student> studentsByRoll = new HashMap<>();
        for (Student student : students.findAll()) {
            studentsByRoll.put(student.rollNo(), student);
        }
        Map<Long, Company> companiesById = new HashMap<>();
        for (Company company : companies.findAll()) {
            companiesById.put(company.id(), company);
        }
        String sql = "SELECT roll_no, company_id, status FROM applications ORDER BY company_id, roll_no";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Application> applications = new ArrayList<>();
            while (rs.next()) {
                Student student = studentsByRoll.get(rs.getString("roll_no"));
                Company company = companiesById.get(rs.getLong("company_id"));
                applications.add(new Application(student, company, ApplicationStatus.valueOf(rs.getString("status"))));
            }
            return applications;
        } catch (SQLException e) {
            throw failure("Could not read applications", e);
        }
    }

    @Override
    public boolean update(Application application) {
        String sql = "UPDATE applications SET status = ? WHERE roll_no = ? AND company_id = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, application.status().name());
            statement.setString(2, application.student().rollNo());
            statement.setLong(3, application.company().id());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw failure("Could not update application " + application, e);
        }
    }

    @Override
    public boolean delete(ApplicationKey key) {
        String sql = "DELETE FROM applications WHERE roll_no = ? AND company_id = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, key.rollNo());
            statement.setLong(2, key.companyId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw failure("Could not delete application " + key, e);
        }
    }

    /** Replaces every stored application in ONE transaction (used to store a drive's results). */
    @Override
    public void replaceAll(List<Application> applications) {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement clear = connection.prepareStatement("DELETE FROM applications")) {
                    clear.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                    for (Application application : applications) {
                        fillInsert(statement, application);
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
                connection.commit();
            } catch (SQLException e) {
                rollback(connection, e);
                throw e;
            } finally {
                endTransaction(connection);
            }
        } catch (SQLException e) {
            throw failure("Transaction rolled back", e);
        }
    }

    private static void fillInsert(PreparedStatement statement, Application application) throws SQLException {
        statement.setString(1, application.student().rollNo());
        statement.setLong(2, application.company().id());
        statement.setString(3, application.status().name());
    }
}
