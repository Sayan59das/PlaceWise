package com.placewise.storage.database;

import com.placewise.exception.PersistenceException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * The application's door to the database: opens connections with {@link DriverManager} and refuses to
 * open new ones after {@link #close()}, which is what "close the DB connection on exit" means here.
 *
 * <p>Connections are not pooled: every repository call opens and closes its own, so there is no shared
 * connection to leak or to corrupt with half-finished transactions.
 */
public final class Database implements ConnectionProvider, AutoCloseable {

    private final DbConfig config;
    private volatile boolean closed;

    /**
     * Creates the database handle (does not connect yet).
     *
     * @param config where and how to connect
     */
    public Database(DbConfig config) {
        this.config = config;
    }

    @Override
    public Connection open() throws SQLException {
        if (closed) {
            throw new PersistenceException("The database has been closed");
        }
        return DriverManager.getConnection(config.url(), config.user(), config.password());
    }

    /**
     * Opens and immediately closes one connection, so a wrong password fails at start-up with a clear
     * message instead of in the middle of a menu action.
     *
     * @throws PersistenceException if the database cannot be reached
     */
    public void verify() {
        try {
            open().close(); // success means the settings are right
        } catch (SQLException e) {
            throw new PersistenceException("Cannot connect to the database (" + config + "): "
                    + e.getMessage(), e);
        }
    }

    /** Stops handing out connections. Idempotent. */
    @Override
    public void close() {
        closed = true;
    }

    /**
     * Tells whether {@link #close()} was called.
     *
     * @return {@code true} once closed
     */
    public boolean isClosed() {
        return closed;
    }
}
