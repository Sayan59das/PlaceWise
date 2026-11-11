package com.placewise.storage.database;

import com.placewise.exception.PersistenceException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates the tables from {@code schema.sql} (a classpath resource shared by MySQL and the H2 tests).
 * Every statement uses {@code CREATE TABLE IF NOT EXISTS}, so running it twice is harmless.
 */
public final class SchemaInitializer {

    private static final String RESOURCE = "/schema.sql";

    private SchemaInitializer() {
    }

    /**
     * Runs the schema script.
     *
     * @param connections where to create the tables
     * @throws PersistenceException if the script cannot be read or a statement fails
     */
    public static void initialize(ConnectionProvider connections) {
        List<String> statements = loadStatements();
        try (Connection connection = connections.open()) {
            for (String sql : statements) {
                // Even DDL goes through PreparedStatement: the project never builds SQL with Statement.
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.execute();
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("Could not create the database schema: " + e.getMessage(), e);
        }
    }

    /**
     * Splits the script into statements: comment lines are dropped and a statement ends at a line
     * ending with a semicolon.
     *
     * @return the statements without their trailing semicolon
     */
    static List<String> loadStatements() {
        List<String> statements = new ArrayList<>();
        try (InputStream in = SchemaInitializer.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new PersistenceException("schema.sql is missing from the application");
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder current = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                current.append(line).append('\n');
                if (trimmed.endsWith(";")) {
                    String sql = current.toString().trim();
                    statements.add(sql.substring(0, sql.length() - 1));
                    current.setLength(0);
                }
            }
        } catch (IOException e) {
            throw new PersistenceException("Could not read schema.sql", e);
        }
        return statements;
    }
}
