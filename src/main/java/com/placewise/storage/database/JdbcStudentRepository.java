package com.placewise.storage.database;

import com.placewise.exception.DuplicateRollNoException;
import com.placewise.model.Student;
import com.placewise.storage.StudentRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Stores students in the {@code students} table using plain JDBC. */
public final class JdbcStudentRepository extends JdbcRepositoryBase implements StudentRepository {

    private static final String COLUMNS = "roll_no, name, branch, semester, cgpa, active_backlogs, email";
    private static final String INSERT = "INSERT INTO students (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE students SET name = ?, branch = ?, semester = ?, cgpa = ?, "
            + "active_backlogs = ?, email = ? WHERE roll_no = ?";

    /** Creates the repository. */
    public JdbcStudentRepository(ConnectionProvider connections) {
        super(connections);
    }

    @Override
    public Student save(Student student) {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(INSERT)) {
            fillInsert(statement, student);
            statement.executeUpdate();
            return student;
        } catch (SQLException e) {
            if (isDuplicateKey(e)) {
                throw new DuplicateRollNoException(student.rollNo(), failure("Duplicate roll number", e));
            }
            throw failure("Could not save student " + student.rollNo(), e);
        }
    }

    /** Saves many students in ONE transaction: either every student is saved, or none is. */
    @Override
    public void saveAll(List<Student> students) {
        Set<String> seen = new HashSet<>();
        for (Student student : students) {
            if (!seen.add(student.rollNo())) {
                throw new DuplicateRollNoException(student.rollNo());
            }
        }
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);     // start the transaction
            try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
                for (Student student : students) {
                    fillInsert(statement, student);
                    statement.addBatch();
                }
                statement.executeBatch();
                connection.commit();             // everything worked: make it permanent
            } catch (SQLException e) {
                rollback(connection, e);         // something failed: undo every row
                throw e;                         // handled by the outer catch below
            } finally {
                endTransaction(connection);
            }
        } catch (SQLException e) {
            if (isDuplicateKey(e)) {
                // Everything was rolled back; now find which roll number was already in the table.
                throw new DuplicateRollNoException(firstExistingRoll(students), failure("Duplicate roll number", e));
            }
            throw failure("Transaction rolled back", e);
        }
    }

    private String firstExistingRoll(List<Student> students) {
        for (Student student : students) {
            if (existsById(student.rollNo())) {
                return student.rollNo();
            }
        }
        return "(unknown)";
    }

    @Override
    public Optional<Student> findById(String rollNo) {
        String sql = "SELECT " + COLUMNS + " FROM students WHERE roll_no = ?";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, rollNo);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(readStudent(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw failure("Could not read student " + rollNo, e);
        }
    }

    @Override
    public List<Student> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM students ORDER BY roll_no";
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Student> students = new ArrayList<>();
            while (rs.next()) {
                students.add(readStudent(rs));
            }
            return students;
        } catch (SQLException e) {
            throw failure("Could not read students", e);
        }
    }

    @Override
    public boolean update(Student student) {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement(UPDATE)) {
            statement.setString(1, student.name());
            statement.setString(2, student.branch());
            statement.setInt(3, student.semester());
            statement.setBigDecimal(4, student.cgpa());
            statement.setInt(5, student.activeBacklogs());
            setNullableString(statement, 6, student.email());
            statement.setString(7, student.rollNo());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw failure("Could not update student " + student.rollNo(), e);
        }
    }

    @Override
    public boolean delete(String rollNo) {
        try (Connection connection = connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM students WHERE roll_no = ?")) {
            statement.setString(1, rollNo);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw failure("Could not delete student " + rollNo, e);
        }
    }

    /** Builds a Student from the current row of a result set. */
    private static Student readStudent(ResultSet rs) throws SQLException {
        return new Student(rs.getString("roll_no"), rs.getString("name"), rs.getString("branch"),
                rs.getInt("semester"), rs.getBigDecimal("cgpa"), rs.getInt("active_backlogs"), rs.getString("email"));
    }

    private static void fillInsert(PreparedStatement statement, Student student) throws SQLException {
        statement.setString(1, student.rollNo());
        statement.setString(2, student.name());
        statement.setString(3, student.branch());
        statement.setInt(4, student.semester());
        statement.setBigDecimal(5, student.cgpa());
        statement.setInt(6, student.activeBacklogs());
        setNullableString(statement, 7, student.email());
    }
}
