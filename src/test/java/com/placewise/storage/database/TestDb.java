package com.placewise.storage.database;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test-only helper: a fresh in-memory H2 database (MySQL mode) built from the real {@code schema.sql},
 * wrapped so the tests can count connections that were opened but never closed.
 */
public final class TestDb implements ConnectionProvider {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final String url;
    private final AtomicInteger opened = new AtomicInteger();
    private final AtomicInteger live = new AtomicInteger();

    private TestDb(String url) {
        this.url = url;
    }

    /** Creates a brand-new empty database with the PlaceWise schema installed. */
    public static TestDb fresh() {
        TestDb db = new TestDb("jdbc:h2:mem:placewise" + COUNTER.incrementAndGet()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        SchemaInitializer.initialize(db);
        return db;
    }

    /** The JDBC URL, for tests that need to build a {@code db.properties}. */
    public String url() {
        return url;
    }

    /** Connections handed out so far. */
    public int totalOpened() {
        return opened.get();
    }

    /** Connections that were handed out and not closed yet. A non-zero value after a test is a leak. */
    public int liveConnections() {
        return live.get();
    }

    @Override
    public Connection open() throws SQLException {
        Connection real = DriverManager.getConnection(url, "sa", "");
        opened.incrementAndGet();
        live.incrementAndGet();
        AtomicBoolean closed = new AtomicBoolean();
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                    if ("close".equals(method.getName()) && closed.compareAndSet(false, true)) {
                        live.decrementAndGet();
                    }
                    try {
                        return method.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }
}
