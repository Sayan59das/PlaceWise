package com.placewise.storage;

import static com.placewise.Fixtures.student;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.placewise.exception.DuplicateRollNoException;
import com.placewise.model.Student;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Behaviour every {@link StudentRepository} must have, whatever it stores into. */
public abstract class StudentRepositoryContract {

    protected StudentRepository repository;

    protected abstract StudentRepository create();

    @BeforeEach
    public void setUp() {
        repository = create();
    }

    public static Student asha() {
        return new Student("R1", "Asha Rao", "CSE", 4, new BigDecimal("8.25"), 1, "asha@uni.edu");
    }

    @Test
    public void saveThenFindRoundTripsEveryField() {
        repository.save(asha());
        assertThat(repository.findById("R1")).contains(asha());
    }

    @Test
    public void optionalEmailSurvivesAsNull() {
        repository.save(student("R2", "Bo", "IT", "7.00", 0));
        assertThat(repository.findById("R2").orElseThrow().email()).isNull();
    }

    @Test
    public void missingIdGivesEmptyNotNullOrException() {
        assertThat(repository.findById("NOPE")).isEmpty();
        assertThat(repository.existsById("NOPE")).isFalse();
    }

    @Test
    public void findAllIsSortedByRollNumber() {
        repository.save(student("R3", "C", "CSE", "7", 0));
        repository.save(student("R1", "A", "CSE", "7", 0));
        repository.save(student("R2", "B", "CSE", "7", 0));
        assertThat(repository.findAll()).extracting(Student::rollNo).containsExactly("R1", "R2", "R3");
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    public void duplicateRollNumberIsRejectedAndKeepsTheOriginal() {
        repository.save(asha());
        assertThatThrownBy(() -> repository.save(student("R1", "Impostor", "IT", "5", 9)))
                .isInstanceOf(DuplicateRollNoException.class)
                .hasMessageContaining("R1");
        assertThat(repository.findById("R1")).contains(asha());
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    public void updateChangesStoredValuesAndReportsMissingRows() {
        repository.save(asha());
        Student changed = new Student("R1", "Asha R.", "IT", 5, new BigDecimal("9.10"), 0, null);
        assertThat(repository.update(changed)).isTrue();
        assertThat(repository.findById("R1")).contains(changed);
        assertThat(repository.update(student("MISSING", "7", 0))).isFalse();
    }

    @Test
    public void deleteRemovesOnceThenReportsFalse() {
        repository.save(asha());
        assertThat(repository.delete("R1")).isTrue();
        assertThat(repository.findById("R1")).isEmpty();
        assertThat(repository.delete("R1")).isFalse();
    }

    @Test
    public void saveAllStoresEveryRow() {
        repository.saveAll(List.of(student("R1", "7", 0), student("R2", "8", 1), student("R3", "9", 2)));
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    public void saveAllWithEmptyListIsANoOp() {
        repository.saveAll(List.of());
        assertThat(repository.count()).isZero();
    }

    @Test
    public void saveAllWithRepeatedRollNumberStoresNothing() {
        assertThatThrownBy(() -> repository.saveAll(List.of(student("R1", "7", 0), student("R2", "7", 0),
                student("R1", "9", 0)))).isInstanceOf(DuplicateRollNoException.class).hasMessageContaining("R1");
        assertThat(repository.count()).isZero();
    }

    @Test
    public void saveAllClashingWithExistingRowStoresNoneOfTheBatch() {
        repository.save(student("R5", "7", 0));
        assertThatThrownBy(() -> repository.saveAll(List.of(student("R1", "7", 0), student("R5", "9", 0),
                student("R9", "8", 0)))).isInstanceOf(DuplicateRollNoException.class).hasMessageContaining("R5");
        assertThat(repository.findAll()).extracting(Student::rollNo).containsExactly("R5");
    }
}
