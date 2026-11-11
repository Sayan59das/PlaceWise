package com.placewise.storage.database;

import com.placewise.exception.PersistenceException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

/**
 * The parent class of the three JDBC repositories. It is an <em>abstract class</em>: it holds the small
 * helpers they all need, and each child class writes its own SQL.
 *
 * <p>Rules every child follows: only {@link PreparedStatement} with {@code ?} placeholders (so user text
 * is never pasted into SQL), and every connection/statement/result set is opened in try-with-resources
 * so it is always closed.
 */
public abstract class JdbcRepositoryBase {

    private final ConnectionProvider connections;

    /** Stores where connections come from (the real database, or H2 in the tests). */
    protected JdbcRepositoryBase(ConnectionProvider connections) {
        this.connections = connections;
    }

    /** Opens a new connection. The caller must close it (use try-with-resources). */
    protected Connection connect() throws SQLException {
        return connections.open();
    }

    /** Turns a checked {@link SQLException} into our own unchecked {@link PersistenceException}. */
    protected static PersistenceException failure(String what, SQLException e) {
        return new PersistenceException(what + ": " + e.getMessage(), e);
    }

    /** Undoes a transaction; if even that fails, the original error stays the main one. */
    protected static void rollback(Connection connection, SQLException cause) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            cause.addSuppressed(rollbackFailure);
        }
    }

    /** Switches auto-commit back on after a transaction, whatever happened. */
    protected static void endTransaction(Connection connection) {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException ignored) {
            // The connection is closed right after this anyway.
        }
    }

    /**
     * Tells whether an error means "this key already exists". H2 reports SQL state 23505; MySQL reports
     * 23000 with error code 1062. The real error can be hidden inside a batch error, so we look at the
     * whole chain of causes and "next" exceptions.
     */
    protected static boolean isDuplicateKey(SQLException error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SQLException sql) {
                for (SQLException s = sql; s != null; s = s.getNextException()) {
                    boolean h2 = "23505".equals(s.getSQLState());
                    boolean mysql = "23000".equals(s.getSQLState()) && s.getErrorCode() == 1062;
                    if (h2 || mysql) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Sets a text parameter, writing SQL NULL when the value is {@code null}. */
    protected static void setNullableString(PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }
}
