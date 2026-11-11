package com.placewise.drive;

import static com.placewise.Fixtures.company;
import static com.placewise.Fixtures.student;
import static com.placewise.drive.DriveFixtures.CHECKER;
import static org.assertj.core.api.Assertions.assertThat;

import com.placewise.model.Application;
import com.placewise.model.ApplicationStatus;
import com.placewise.model.Company;
import com.placewise.model.Student;
import com.placewise.validation.CompanyValidator;
import com.placewise.validation.StudentValidator;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The invariant checker must actually detect each kind of corruption; otherwise "the invariants hold"
 * in the drive tests would prove nothing. Each test builds a deliberately broken result.
 */
class DriveInvariantsTest {

    private final Company acme = company(1, "Acme", "7.00", 0, 2);
    private final Student top = student("A", "9.50", 0);
    private final Student mid = student("B", "8.50", 0);
    private final Student low = student("C", "7.50", 0);
    private final Student weak = student("D", "5.00", 0);

    private static Application app(Student s, Company c, ApplicationStatus status) {
        return new Application(s, c, status);
    }

    private DriveResult result(List<Application> applications, List<Application> selected,
                               List<Application> waitlisted) {
        return new DriveResult(DriveConfig.defaults(), List.of(acme), applications,
                List.of(new BoardSnapshot(acme, selected, waitlisted)), Duration.ZERO);
    }

    private List<String> violations(DriveResult result) {
        return DriveInvariants.check(result, CHECKER);
    }

    @Test
    void aCorrectResultHasNoViolations() {
        Application a = app(top, acme, ApplicationStatus.SELECTED);
        Application b = app(mid, acme, ApplicationStatus.SELECTED);
        Application c = app(low, acme, ApplicationStatus.WAITLISTED);
        Application d = app(weak, acme, ApplicationStatus.REJECTED);
        assertThat(violations(result(List.of(a, b, c, d), List.of(a, b), List.of(c)))).isEmpty();
    }

    @Test
    void detectsMoreSelectedThanSeats() {
        Application a = app(top, acme, ApplicationStatus.SELECTED);
        Application b = app(mid, acme, ApplicationStatus.SELECTED);
        Application c = app(low, acme, ApplicationStatus.SELECTED);
        assertThat(violations(result(List.of(a, b, c), List.of(a, b, c), List.of())))
                .anyMatch(v -> v.contains("3 selected but only 2 seats"));
    }

    @Test
    void detectsAFreeSeatWhileSomeoneIsWaitlisted() {
        Application a = app(top, acme, ApplicationStatus.SELECTED);
        Application c = app(low, acme, ApplicationStatus.WAITLISTED);
        assertThat(violations(result(List.of(a, c), List.of(a), List.of(c))))
                .anyMatch(v -> v.contains("free seat while students are waitlisted"));
    }

    @Test
    void detectsAWaitlistedStudentWhoOutranksASelectedOne() {
        Application a = app(low, acme, ApplicationStatus.SELECTED);
        Application b = app(mid, acme, ApplicationStatus.SELECTED);
        Application c = app(top, acme, ApplicationStatus.WAITLISTED);
        assertThat(violations(result(List.of(a, b, c), List.of(b, a), List.of(c))))
                .anyMatch(v -> v.contains("outranks selected"));
    }

    @Test
    void detectsAnApplicationThatNeverReachedAFinalStatus() {
        Application a = app(top, acme, ApplicationStatus.APPLIED);
        assertThat(violations(result(List.of(a), List.of(), List.of())))
                .anyMatch(v -> v.contains("never reached a final status"));
    }

    @Test
    void detectsAnIneligibleStudentHoldingASeat() {
        Application a = app(weak, acme, ApplicationStatus.SELECTED);
        assertThat(violations(result(List.of(a), List.of(a), List.of())))
                .anyMatch(v -> v.contains("student is not eligible"));
    }

    @Test
    void detectsAnEligibleStudentRejectedWithoutBeingADuplicate() {
        Application a = app(top, acme, ApplicationStatus.REJECTED);
        assertThat(violations(result(List.of(a), List.of(), List.of())))
                .anyMatch(v -> v.contains("rejected although eligible and not a duplicate"));
    }

    @Test
    void anEligibleRejectionIsFineWhenItIsTheDuplicateOfALiveApplication() {
        Application live = app(top, acme, ApplicationStatus.SELECTED);
        Application duplicate = app(top, acme, ApplicationStatus.REJECTED);
        assertThat(violations(result(List.of(live, duplicate), List.of(live), List.of()))).isEmpty();
    }

    @Test
    void detectsTheSameStudentSelectedTwiceAtOneCompany() {
        Application a = app(top, acme, ApplicationStatus.SELECTED);
        Application again = app(top, acme, ApplicationStatus.SELECTED);
        List<String> found = violations(result(List.of(a, again), List.of(a, again), List.of()));
        assertThat(found).anyMatch(v -> v.contains("selected 2 times at company 1"))
                .anyMatch(v -> v.contains("holds 2 live applications"));
    }

    @Test
    void detectsDisagreementBetweenStatusesAndBoards() {
        Application a = app(top, acme, ApplicationStatus.SELECTED);
        Application c = app(low, acme, ApplicationStatus.WAITLISTED);
        // statuses say a is selected and c waitlisted, but the board lists them the other way round
        List<String> found = violations(result(List.of(a, c), List.of(c), List.of(a)));
        assertThat(found).anyMatch(v -> v.contains("disagree about the seat"))
                .anyMatch(v -> v.contains("disagree about the waitlist"));
    }

    @Test
    void generatedDataIsValidDeterministicAndVaried() {
        List<Student> first = DriveDataGenerator.students(300, 7);
        assertThat(first).isEqualTo(DriveDataGenerator.students(300, 7));
        assertThat(first).isNotEqualTo(DriveDataGenerator.students(300, 8));
        assertThat(first).allSatisfy(s -> assertThat(StudentValidator.problems(s)).isEmpty());
        assertThat(first.stream().map(Student::rollNo).distinct().count()).isEqualTo(300);
        assertThat(first.stream().map(Student::branch).distinct().count()).isGreaterThan(2);
        assertThat(first.stream().map(Student::cgpa).distinct().count()).isLessThan(150); // plenty of ties

        List<Company> companies = DriveDataGenerator.companies(10, 6, 7);
        assertThat(companies).isEqualTo(DriveDataGenerator.companies(10, 6, 7));
        assertThat(companies).allSatisfy(c -> assertThat(CompanyValidator.problems(c)).isEmpty());
        assertThat(companies).extracting(Company::id).containsExactly(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L);
        assertThat(companies.get(9).name()).isEqualTo("Company 10");
        List<String> names = new ArrayList<>(companies.stream().map(Company::name).toList());
        assertThat(names).doesNotHaveDuplicates();
        assertThat(companies).anyMatch(c -> c.allowedBranches().isEmpty()).anyMatch(c -> !c.allowedBranches().isEmpty());
    }
}
