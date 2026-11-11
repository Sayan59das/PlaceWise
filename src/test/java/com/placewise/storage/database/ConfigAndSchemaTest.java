package com.placewise.storage.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.exception.PersistenceException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigAndSchemaTest {

    @TempDir
    Path tmp;

    @Test
    void dbConfigReadsAPropertiesFile() throws IOException {
        Path file = tmp.resolve("db.properties");
        Files.writeString(file, "db.url=jdbc:h2:mem:x\ndb.user= sa \ndb.password=s3cret\n");
        DbConfig config = DbConfig.load(file);
        assertThat(config.url()).isEqualTo("jdbc:h2:mem:x");
        assertThat(config.user()).isEqualTo("sa");
        assertThat(config.password()).isEqualTo("s3cret");
    }

    @Test
    void passwordNeverAppearsInToString() {
        assertThat(new DbConfig("jdbc:x", "bob", "hunter2").toString()).contains("bob").doesNotContain("hunter2");
    }

    @Test
    void missingFileAndMissingUrlAreReportedAsPersistenceExceptions() throws IOException {
        assertThatThrownBy(() -> DbConfig.load(tmp.resolve("absent.properties")))
                .isInstanceOf(PersistenceException.class).hasMessageContaining("absent.properties");
        Path noUrl = tmp.resolve("empty.properties");
        Files.writeString(noUrl, "db.user=sa\n");
        assertThatThrownBy(() -> DbConfig.load(noUrl)).isInstanceOf(PersistenceException.class)
                .hasMessageContaining("db.url");
        assertThatThrownBy(() -> DbConfig.fromProperties(new Properties())).isInstanceOf(PersistenceException.class);
    }

    @Test
    void userAndPasswordDefaultToEmpty() {
        Properties p = new Properties();
        p.setProperty("db.url", "jdbc:h2:mem:y");
        DbConfig config = DbConfig.fromProperties(p);
        assertThat(config.user()).isEmpty();
        assertThat(config.password()).isEmpty();
    }

    @Test
    void schemaScriptHasFourCreateStatementsAndRunsTwiceWithoutError() {
        List<String> statements = SchemaInitializer.loadStatements();
        assertThat(statements).hasSize(4).allSatisfy(s -> assertThat(s).startsWith("CREATE TABLE IF NOT EXISTS"));
        assertThat(statements).noneMatch(s -> s.endsWith(";") || s.contains("--"));
        TestDb db = TestDb.fresh();          // already ran once
        SchemaInitializer.initialize(db);    // second run must be harmless
        assertThat(db.liveConnections()).isZero();
    }

    @Test
    void schemaFailureBecomesAPersistenceException() {
        ConnectionProvider broken = () -> {
            throw new java.sql.SQLException("no database today");
        };
        assertThatThrownBy(() -> SchemaInitializer.initialize(broken)).isInstanceOf(PersistenceException.class)
                .hasMessageContaining("no database today");
    }

    @Test
    void databaseRefusesNewConnectionsAfterClose() throws Exception {
        TestDb h2 = TestDb.fresh();
        Database database = new Database(new DbConfig(h2.url(), "sa", ""));
        database.verify();
        try (Connection c = database.open()) {
            assertThat(c.isValid(1)).isTrue();
        }
        assertThat(database.isClosed()).isFalse();
        database.close();
        database.close(); // idempotent
        assertThat(database.isClosed()).isTrue();
        assertThatThrownBy(database::open).isInstanceOf(PersistenceException.class).hasMessageContaining("closed");
    }

    @Test
    void verifyReportsUnreachableDatabasesClearly() {
        Database database = new Database(new DbConfig("jdbc:h2:tcp://localhost:1/nothing", "sa", "pw"));
        assertThatThrownBy(database::verify).isInstanceOf(PersistenceException.class)
                .hasMessageContaining("Cannot connect").hasMessageNotContaining("pw");
    }
}
