package com.placewise.storage;

import static com.placewise.Fixtures.company;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.exception.InvalidCompanyException;
import com.placewise.model.Company;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Behaviour every {@link CompanyRepository} must have. */
public abstract class CompanyRepositoryContract {

    protected CompanyRepository repository;

    protected abstract CompanyRepository create();

    @BeforeEach
    public void setUp() {
        repository = create();
    }

    @Test
    public void saveAssignsIdsAndRoundTripsBranches() {
        Company saved = repository.save(company("Acme", "7.50", 1, 5, "IT", "CSE"));
        assertThat(saved.id()).isPositive();
        Company loaded = repository.findById(saved.id()).orElseThrow();
        assertThat(loaded).isEqualTo(saved);
        assertThat(loaded.allowedBranches()).containsExactly("CSE", "IT");
    }

    @Test
    public void emptyBranchListRoundTripsAsEmpty() {
        Company saved = repository.save(company("Open", "6", 0, 2));
        assertThat(repository.findById(saved.id()).orElseThrow().allowedBranches()).isEmpty();
    }

    @Test
    public void idsAreDistinctAndFindAllKeepsIdOrderWithBranchesAttached() {
        Company a = repository.save(company("A", "6", 0, 1, "CSE"));
        Company b = repository.save(company("B", "7", 0, 1));
        Company c = repository.save(company("C", "8", 0, 1, "ECE", "IT"));
        assertThat(a.id()).isLessThan(b.id());
        assertThat(b.id()).isLessThan(c.id());
        assertThat(repository.findAll()).containsExactly(a, b, c);
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    public void missingIdGivesEmpty() {
        assertThat(repository.findById(999L)).isEmpty();
        assertThat(repository.findByName("Ghost")).isEmpty();
    }

    @Test
    public void findByNameIgnoresCase() {
        Company saved = repository.save(company("Acme Corp", "7", 0, 1));
        assertThat(repository.findByName("  ACME corp ")).contains(saved);
    }

    @Test
    public void updateReplacesFieldsAndBranches() {
        Company saved = repository.save(company("Acme", "7", 0, 2, "CSE", "IT"));
        Company changed = company(saved.id(), "Acme Ltd", "8.25", 2, 9, "ECE");
        assertThat(repository.update(changed)).isTrue();
        assertThat(repository.findById(saved.id())).contains(changed);
        assertThat(repository.findById(saved.id()).orElseThrow().allowedBranches()).containsExactly("ECE");
    }

    @Test
    public void updateToEmptyBranchesMeansAllBranches() {
        Company saved = repository.save(company("Acme", "7", 0, 2, "CSE"));
        repository.update(company(saved.id(), "Acme", "7", 0, 2));
        assertThat(repository.findById(saved.id()).orElseThrow().allowedBranches()).isEmpty();
    }

    @Test
    public void updateOfMissingCompanyReturnsFalse() {
        assertThat(repository.update(company(404, "Ghost", "7", 0, 1))).isFalse();
    }

    @Test
    public void deleteRemovesTheCompanyAndItsBranches() {
        Company saved = repository.save(company("Acme", "7", 0, 2, "CSE"));
        assertThat(repository.delete(saved.id())).isTrue();
        assertThat(repository.findById(saved.id())).isEmpty();
        assertThat(repository.delete(saved.id())).isFalse();
        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    public void duplicateNameIsRejected() {
        repository.save(company("Acme", "7", 0, 1));
        assertThatThrownBy(() -> repository.save(company("Acme", "8", 1, 2)))
                .isInstanceOf(InvalidCompanyException.class).hasMessageContaining("Acme");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    public void renamingToAnExistingNameIsRejected() {
        repository.save(company("Acme", "7", 0, 1));
        Company other = repository.save(company("Beta", "7", 0, 1));
        assertThatThrownBy(() -> repository.update(company(other.id(), "Acme", "7", 0, 1)))
                .isInstanceOf(InvalidCompanyException.class);
        assertThat(repository.findById(other.id()).orElseThrow().name()).isEqualTo("Beta");
    }
}
