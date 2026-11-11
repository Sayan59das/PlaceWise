package com.placewise.storage.database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Hands out a <em>new</em> JDBC connection on every call. The caller owns it and must close it,
 * which in this code base always means a try-with-resources block.
 *
 * <p>It is a functional interface, so tests can pass a lambda such as
 * {@code () -> DriverManager.getConnection(h2Url)}.
 */
@FunctionalInterface
public interface ConnectionProvider {

    /**
     * Opens a connection.
     *
     * @return a fresh connection in auto-commit mode
     * @throws SQLException if the database cannot be reached
     */
    Connection open() throws SQLException;
}
