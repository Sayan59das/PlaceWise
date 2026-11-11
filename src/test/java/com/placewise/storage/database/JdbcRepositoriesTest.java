package com.placewise.storage.database;

import static com.placewise.Fixtures.company;
import static com.placewise.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.exception.DuplicateRollNoException;
import com.placewise.exception.PersistenceException;
import com.placewise.model.Application;
import com.placewise.model.ApplicationKey;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.storage.ApplicationRepository;
import com.placewise.storage.CompanyRepository;
import com.placewise.storage.CompanyRepositoryContract;
import com.placewise.storage.StudentRepository;
import com.placewise.storage.StudentRepositoryContract;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The JDBC repositories against H2 in MySQL mode, using the real schema.sql. */
class JdbcRepositoriesTest {

    @Nested
    class Students extends StudentRepositoryContract {
        private TestDb db;

        @Override
        protected StudentRepository create() {
            db = TestDb.fresh();
            return new JdbcStudentRepository(db);
        }

        @Test
        void sqlInjectionTextIsStoredLiterallyAndTheTableSurvives() {
            String evil = "Robert'); DROP TABLE students;--";
            Student s = new Student("X1' OR '1'='1", evil, "CSE", 3, new BigDecimal("7.00"), 0, null);
            repository.save(s);
            assertThat(repository.findById("X1' OR '1'='1")).contains(s);
            assertThat(repository.findById("X1' OR '1'='1").orElseThrow().name()).isEqualTo(evil);
            // the classic attack string used as a *lookup key* matches nothing instead of everything
            assertThat(repository.findById("' OR '1'='1")).isEmpty();
            assertThat(repository.update(s.withCgpa(new BigDecimal("8")))).isTrue();
            assertThat(repository.count()).isEqualTo(1); // table still exists and holds exactly our row
        }

        @Test
        void aBadRowRollsTheWholeBatchBack() {
            Student good = student("R1", "7", 0);
            Student bad = new Student("R2", "Bad", "CSE", 3, new BigDecimal("11.00"), 0, null); // CHECK fails
            assertThatThrownBy(() -> repository.saveAll(List.of(good, bad)))
                    .isInstanceOf(PersistenceException.class);
            assertThat(repository.count()).isZero();
        }

        @Test
        void aDatabaseDuplicateDuringBatchRollsBackAndNamesTheOffender() {
            repository.save(student("R2", "7", 0));
            assertThatThrownBy(() -> repository.saveAll(List.of(student("R1", "7", 0), student("R2", "8", 0))))
                    .isInstanceOf(DuplicateRollNoException.class)
                    .hasMessageContaining("R2")
                    .hasCauseInstanceOf(PersistenceException.class);
            assertThat(repository.findAll()).extracting(Student::rollNo).containsExactly("R2");
        }

        @Test
        void noConnectionIsLeakedByAnyOperationSuccessfulOrFailing() {
            repository.save(student("R1", "7", 0));
            repository.findById("R1");
            repository.findAll();
            repository.update(student("R1", "8", 0));
            assertThatThrownBy(() -> repository.save(student("R1", "7", 0)));
            assertThatThrownBy(() -> repository.saveAll(List.of(student("R1", "7", 0))));
            assertThatThrownBy(() -> repository.saveAll(List.of(new Student("R7", "x", "CSE", 3,
                    new BigDecimal("12.00"), 0, null))));
            repository.delete("R1");
            assertThat(db.totalOpened()).isGreaterThan(5);
            assertThat(db.liveConnections()).as("connections opened but never closed").isZero();
        }

        @Test
        void autoCommitIsRestoredAfterAFailedTransaction() throws SQLException {
            Connection[] holder = new Connection[1];
            ConnectionProvider keepOpen = () -> {
                Connection real = db.open();
                holder[0] = real;
                // hand out a connection whose close() does nothing, so the test can inspect it afterwards
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                        new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                            if ("close".equals(method.getName())) {
                                return null;
                            }
                            try {
                                return method.invoke(real, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                        });
            };
            StudentRepository repo = new JdbcStudentRepository(keepOpen);
            Student bad = new Student("R2", "Bad", "CSE", 3, new BigDecimal("11.00"), 0, null);
            assertThatThrownBy(() -> repo.saveAll(List.of(student("R1", "7", 0), bad)))
                    .isInstanceOf(PersistenceException.class);
            assertThat(holder[0].getAutoCommit()).isTrue();
            holder[0].close();
        }

        @Test
        void aRealDatabaseStillHasOnlyOneRowPerRollNumberAfterManyInserts() {
            List<Student> many = new ArrayList<>();
            for (int i = 0; i < 2_000; i++) {
                many.add(student(String.format("B%05d", i), "7." + (i % 100 < 10 ? "0" : "") + (i % 100), i % 3));
            }
            repository.saveAll(many);
            assertThat(repository.count()).isEqualTo(2_000);
        }
    }

    @Nested
    class Companies extends CompanyRepositoryContract {
        @Override
        protected CompanyRepository create() {
            return new JdbcCompanyRepository(TestDb.fresh());
        }

        @Test
        void deletingACompanyCascadesToBranchRows() throws SQLException {
            TestDb db = TestDb.fresh();
            CompanyRepository repo = new JdbcCompanyRepository(db);
            Company saved = repo.save(company("Acme", "7", 0, 2, "CSE", "IT"));
            repo.delete(saved.id());
            try (Connection c = db.open();
                 PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM company_branches");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            }
            assertThat(db.liveConnections()).isZero();
        }

        @Test
        void sqlInjectionInACompanyNameIsStoredLiterally() {
            Company saved = repository.save(company("x'); DELETE FROM companies;--", "7", 0, 1));
            assertThat(repository.findById(saved.id()).orElseThrow().name()).isEqualTo("x'); DELETE FROM companies;--");
            assertThat(repository.findByName("x'); delete from companies;--")).isPresent();
            assertThat(repository.count()).isEqualTo(1);
        }
    }

    @Nested
    class Applications {
        private final TestDb db = TestDb.fresh();
        private final StudentRepository students = new JdbcStudentRepository(db);
        private final CompanyRepository companies = new JdbcCompanyRepository(db);
        private final ApplicationRepository repository = new JdbcApplicationRepository(db, students, companies);
        private Student ann;
        private Company acme;

        private void seed() {
            ann = students.save(student("R1", "8", 0));
            students.save(student("R2", "9", 0));
            acme = companies.save(company("Acme", "7", 0, 2, "CSE"));
        }

        @Test
        void crudRoundTrip() {
            seed();
            Application app = new Application(ann, acme, ApplicationStatus.WAITLISTED);
            repository.save(app);
            Application loaded = repository.findById(app.key()).orElseThrow();
            assertThat(loaded.status()).isEqualTo(ApplicationStatus.WAITLISTED);
            assertThat(loaded.student()).isEqualTo(ann);
            assertThat(loaded.company()).isEqualTo(acme);
            app.setStatus(ApplicationStatus.SELECTED);
            assertThat(repository.update(app)).isTrue();
            assertThat(repository.findAll()).extracting(Application::status).containsExactly(ApplicationStatus.SELECTED);
            assertThat(repository.delete(app.key())).isTrue();
            assertThat(repository.delete(app.key())).isFalse();
            assertThat(repository.findById(new ApplicationKey("R1", acme.id()))).isEmpty();
        }

        @Test
        void duplicateApplicationIsRejected() {
            seed();
            repository.save(new Application(ann, acme));
            assertThatThrownBy(() -> repository.save(new Application(ann, acme)))
                    .isInstanceOf(PersistenceException.class).hasMessageContaining("already exists");
        }

        @Test
        void foreignKeyStopsApplicationsForUnknownStudents() {
            seed();
            Application ghost = new Application(student("GHOST", "8", 0), acme);
            assertThatThrownBy(() -> repository.save(ghost)).isInstanceOf(PersistenceException.class);
        }

        @Test
        void replaceAllIsAtomic() {
            seed();
            repository.save(new Application(ann, acme));
            Application ok = new Application(students.findById("R2").orElseThrow(), acme, ApplicationStatus.SELECTED);
            Application ghost = new Application(student("GHOST", "8", 0), acme);
            assertThatThrownBy(() -> repository.replaceAll(List.of(ok, ghost))).isInstanceOf(PersistenceException.class);
            assertThat(repository.findAll()).extracting(a -> a.student().rollNo()).containsExactly("R1"); // old data kept
            repository.replaceAll(List.of(ok));
            assertThat(repository.findAll()).extracting(a -> a.student().rollNo()).containsExactly("R2");
        }

        @Test
        void deletingAStudentOrCompanyCascadesToItsApplications() {
            seed();
            repository.save(new Application(ann, acme));
            repository.save(new Application(students.findById("R2").orElseThrow(), acme));
            students.delete("R1");
            assertThat(repository.findAll()).extracting(a -> a.student().rollNo()).containsExactly("R2");
            companies.delete(acme.id());
            assertThat(repository.findAll()).isEmpty();
            assertThat(db.liveConnections()).isZero();
        }
    }
}
