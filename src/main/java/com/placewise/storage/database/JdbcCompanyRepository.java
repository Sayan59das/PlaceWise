package com.placewise.storage.database;

import com.placewise.exception.InvalidCompanyException;
import com.placewise.model.Company;
import com.placewise.storage.CompanyRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Stores companies using plain JDBC. A company is one row in {@code companies} plus one row per allowed
 * branch in {@code company_branches}, so saving or updating a company is a transaction (both or nothing).
 */
public final class JdbcCompanyRepository extends JdbcRepositoryBase implements CompanyRepository {

    private static final String COLUMNS = "id, name, min_cgpa, max_backlogs, package_lpa, seats";
    private static final String INSERT =
            "INSERT INTO companies (name, min_cgpa, max_backlogs, package_lpa, seats) VALUES (?, ?, ?, ?, ?)";
    private static final String UPDATE =
            "UPDATE companies SET name = ?, min_cgpa = ?, max_backlogs = ?, package_lpa = ?, seats = ? WHERE id = ?";

    /** Creates the repository. */
    public JdbcCompanyRepository(ConnectionProvider connections) {
        super(connections);
    }

    @Override
    public Company save(Company company) {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                long id = insertCompanyRow(connection, company);
                insertBranches(connection, id, company.allowedBranches());
                connection.commit();
                return company.withId(id);
            } catch (SQLException e) {
                rollback(connection, e);
                throw e;
            } finally {
                endTransaction(connection);
            }
        } catch (SQLException e) {
            throw companyFailure(company, e);
        }
    }

    @Override
    public boolean update(Company company) {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                int rows;
                try (PreparedStatement statement = connection.prepareStatement(UPDATE)) {
                    fillCompany(statement, company);
                    statement.setLong(6, company.id());
                    rows = statement.executeUpdate();
                }
                if (rows == 0) {
                    connection.rollback();
                    return false; // no company with that id
                }
                // Replace the branch rows: delete the old ones, insert the new ones.
                try (PreparedStatement statement =
                             connection.prepareStatement("DELETE FROM company_branches WHERE company_id = ?")) {
                    statement.setLong(1, company.id());
                    statement.executeUpdate();
                }
                insertBranches(connection, company.id(), company.allowedBranches());
                connection.commit();
                return true;
            } catch (SQLException e) {
                rollback(connection, e);
                throw e;
            } finally {
                endTransaction(connection);
            }
        } catch (SQLException e) {
            throw companyFailure(company, e);
        }
    }

    @Override
    public Optional<Company> findById(Long id) {
        String sql = "SELECT " + COLUMNS + " FROM companies WHERE id = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            Company company;
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                company = readCompany(rs);
            }
            Set<String> branches = new TreeSet<>();
            try (PreparedStatement branchQuery =
                         connection.prepareStatement("SELECT branch FROM company_branches WHERE company_id = ?")) {
                branchQuery.setLong(1, id);
                try (ResultSet rs = branchQuery.executeQuery()) {
                    while (rs.next()) {
                        branches.add(rs.getString("branch"));
                    }
                }
            }
            return Optional.of(company.withAllowedBranches(branches));
        } catch (SQLException e) {
            throw failure("Could not read company " + id, e);
        }
    }

    @Override
    public Optional<Company> findByName(String name) {
        Long id = null;
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("SELECT id FROM companies WHERE LOWER(name) = ?")) {
            statement.setString(1, name.trim().toLowerCase(Locale.ROOT));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    id = rs.getLong("id");
                }
            }
        } catch (SQLException e) {
            throw failure("Could not search companies", e);
        }
        if (id == null) {
            return Optional.empty();
        }
        return findById(id);
    }

    @Override
    public List<Company> findAll() {
        try (Connection connection = connect()) {
            // Query 1: every company row.
            List<Company> companies = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT " + COLUMNS + " FROM companies ORDER BY id");
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    companies.add(readCompany(rs));
                }
            }
            // Query 2: every branch row at once (instead of one extra query per company).
            Map<Long, Set<String>> branchesByCompany = new HashMap<>();
            try (PreparedStatement statement = connection.prepareStatement("SELECT company_id, branch FROM company_branches");
                 ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    long companyId = rs.getLong("company_id");
                    Set<String> branches = branchesByCompany.get(companyId);
                    if (branches == null) {
                        branches = new TreeSet<>();
                        branchesByCompany.put(companyId, branches);
                    }
                    branches.add(rs.getString("branch"));
                }
            }
            // Attach each company's branches.
            List<Company> result = new ArrayList<>();
            for (Company company : companies) {
                Set<String> branches = branchesByCompany.getOrDefault(company.id(), Set.of());
                result.add(company.withAllowedBranches(branches));
            }
            return result;
        } catch (SQLException e) {
            throw failure("Could not read companies", e);
        }
    }

    @Override
    public boolean delete(Long id) {
        // The company's branch rows and applications are removed by ON DELETE CASCADE in the schema.
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM companies WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw failure("Could not delete company " + id, e);
        }
    }

    /** Inserts the company row and returns the id the database generated for it. */
    private static long insertCompanyRow(Connection connection, Company company) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            fillCompany(statement, company);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("The database returned no generated id");
                }
                return keys.getLong(1);
            }
        }
    }

    private static void insertBranches(Connection connection, long companyId, Set<String> branches)
            throws SQLException {
        if (branches.isEmpty()) {
            return; // no rows means "all branches allowed"
        }
        String sql = "INSERT INTO company_branches (company_id, branch) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String branch : branches) {
                statement.setLong(1, companyId);
                statement.setString(2, branch);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    /** A duplicate company name becomes a clear message; anything else a PersistenceException. */
    private static RuntimeException companyFailure(Company company, SQLException e) {
        if (isDuplicateKey(e)) {
            return new InvalidCompanyException("Company name already exists: " + company.name());
        }
        return failure("Could not save company " + company.name(), e);
    }

    /** Builds a Company (without branches) from the current row of a result set. */
    private static Company readCompany(ResultSet rs) throws SQLException {
        return new Company(rs.getLong("id"), rs.getString("name"), rs.getBigDecimal("min_cgpa"),
                rs.getInt("max_backlogs"), Set.of(), rs.getBigDecimal("package_lpa"), rs.getInt("seats"));
    }

    private static void fillCompany(PreparedStatement statement, Company company) throws SQLException {
        statement.setString(1, company.name());
        statement.setBigDecimal(2, company.minCgpa());
        statement.setInt(3, company.maxBacklogs());
        statement.setBigDecimal(4, company.packageLpa());
        statement.setInt(5, company.seats());
    }
}
