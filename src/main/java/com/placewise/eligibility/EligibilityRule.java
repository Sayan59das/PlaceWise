package com.placewise.eligibility;

import com.placewise.model.Company;
import com.placewise.model.Student;

/**
 * One eligibility criterion (for example "CGPA must be at least the minimum").
 *
 * <p>It has exactly one abstract method, so it is a <em>functional interface</em>: a rule can be a normal
 * class such as {@link CgpaRule}, or a short lambda such as {@code (s, c) -> Result.fail("closed")}.
 */
@FunctionalInterface
public interface EligibilityRule {

    /** Checks one student against one company and says pass or fail, with a reason. */
    Result check(Student student, Company company);

    /**
     * The outcome of one rule.
     *
     * @param passed {@code true} if the student satisfies the rule
     * @param reason a sentence such as {@code "CGPA 6.80 is below required 7.50"}
     */
    record Result(boolean passed, String reason) {

        /** Creates a passing result. */
        public static Result pass(String reason) {
            return new Result(true, reason);
        }

        /** Creates a failing result. */
        public static Result fail(String reason) {
            return new Result(false, reason);
        }
    }
}
