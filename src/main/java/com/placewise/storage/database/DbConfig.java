package com.placewise.storage.database;

import com.placewise.exception.PersistenceException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Database connection settings, read from a {@code db.properties} file with {@link Properties}.
 * The file holds credentials, so it is git-ignored; only {@code db.properties.example} is committed.
 */
public final class DbConfig {

    private final String url;
    private final String user;
    private final String password;

    /**
     * Creates a configuration directly (used by tests).
     *
     * @param url      JDBC URL
     * @param user     database user
     * @param password database password
     */
    public DbConfig(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /**
     * Reads a properties file through a character stream ({@link FileReader}).
     *
     * @param file path to {@code db.properties}
     * @return the configuration
     * @throws PersistenceException if the file is unreadable or {@code db.url} is missing
     */
    public static DbConfig load(Path file) {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(file.toFile(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new PersistenceException("Cannot read database settings from " + file + ": "
                    + e.getMessage(), e);
        }
        return fromProperties(properties);
    }

    /**
     * Builds a configuration from already loaded properties.
     *
     * @param properties must contain {@code db.url}; {@code db.user} and {@code db.password} default to empty
     * @return the configuration
     * @throws PersistenceException if {@code db.url} is missing or blank
     */
    public static DbConfig fromProperties(Properties properties) {
        String url = properties.getProperty("db.url", "").trim();
        if (url.isEmpty()) {
            throw new PersistenceException("db.url is missing in the database settings");
        }
        return new DbConfig(url, properties.getProperty("db.user", "").trim(),
                properties.getProperty("db.password", ""));
    }

    /**
     * Returns the JDBC URL.
     *
     * @return the URL
     */
    public String url() {
        return url;
    }

    /**
     * Returns the user name.
     *
     * @return the user
     */
    public String user() {
        return user;
    }

    /**
     * Returns the password. Not printed anywhere; see {@link #toString()}.
     *
     * @return the password
     */
    public String password() {
        return password;
    }

    /**
     * Describes the configuration without the password, so it is safe to print or log.
     *
     * @return URL and user only
     */
    @Override
    public String toString() {
        return "DbConfig[url=" + url + ", user=" + user + ", password=***]";
    }
}
