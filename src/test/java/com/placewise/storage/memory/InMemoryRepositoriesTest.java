package com.placewise.storage.memory;

import static com.placewise.Fixtures.company;
import static com.placewise.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.exception.InvalidCompanyException;
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
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** The in-memory repositories obey the same contracts as the JDBC ones. */
class InMemoryRepositoriesTest {

    @Nested
    class Students extends StudentRepositoryContract {
        @Override
        protected StudentRepository create() {
            return new InMemoryStudentRepository();
        }
    }

    @Nested
    class Companies extends CompanyRepositoryContract {
        @Override
        protected CompanyRepository create() {
            return new InMemoryCompanyRepository();
        }

        @Test
        void nameClashIsCaseInsensitive() {
            repository.save(company("Acme", "7", 0, 1));
            assertThatThrownBy(() -> repository.save(company("ACME", "7", 0, 1)))
                    .isInstanceOf(com.placewise.exception.InvalidCompanyException.class);
        }
    }

    @Nested
    class Applications {
        private final Student ann = student("R1", "8", 0);
        private final Company acme = company(1, "Acme", "7", 0, 2);
        private final ApplicationRepository repository = new InMemoryApplicationRepository();

        @Test
        void crudRoundTrip() {
            Application app = new Application(ann, acme);
            repository.save(app);
            assertThat(repository.findById(new ApplicationKey("R1", 1))).containsSame(app);
            app.setStatus(ApplicationStatus.SELECTED);
            assertThat(repository.update(app)).isTrue();
            assertThat(repository.findAll()).hasSize(1);
            assertThat(repository.delete(app.key())).isTrue();
            assertThat(repository.delete(app.key())).isFalse();
            assertThat(repository.update(app)).isFalse();
        }

        @Test
        void duplicateKeyIsRejected() {
            repository.save(new Application(ann, acme));
            assertThatThrownBy(() -> repository.save(new Application(ann, acme)))
                    .isInstanceOf(PersistenceException.class);
        }

        @Test
        void replaceAllSwapsTheWholeContent() {
            repository.save(new Application(ann, acme));
            Application other = new Application(student("R2", "9", 0), acme, ApplicationStatus.WAITLISTED);
            repository.replaceAll(List.of(other));
            assertThat(repository.findAll()).containsExactly(other);
        }
    }
}
