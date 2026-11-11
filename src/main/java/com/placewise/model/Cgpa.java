package com.placewise.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Helpers for CGPA values.
 *
 * <p>CGPA is a {@link BigDecimal} with exactly two decimals. {@code double} is avoided because
 * {@code 7.5} and {@code 7.499999999999999} are different doubles but the same academic result, and
 * because {@code 0.1 + 0.2 != 0.3}: an eligibility check that says "CGPA >= minimum" must be exact on the
 * boundary.
 */
public final class Cgpa {

    /** Lowest legal CGPA, {@code 0.00}. */
    public static final BigDecimal MIN = new BigDecimal("0.00");

    /** Highest legal CGPA, {@code 10.00}. */
    public static final BigDecimal MAX = new BigDecimal("10.00");

    /** Number of decimals every stored CGPA has. */
    public static final int SCALE = 2;

    private Cgpa() {
    }

    /**
     * Rounds a value to two decimals (half-up). It does <em>not</em> check the 0..10 range; that is the
     * job of the validators, so an out-of-range value can still be reported with a clear message.
     *
     * @param value the raw value, never {@code null}
     * @return the same number with scale 2
     */
    public static BigDecimal normalize(BigDecimal value) {
        Objects.requireNonNull(value, "cgpa");
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Parses text such as {@code "8.5"} into a normalized CGPA.
     *
     * @param text the text, surrounding spaces are ignored
     * @return the normalized value
     * @throws NumberFormatException if the text is not a decimal number
     */
    public static BigDecimal parse(String text) {
        return normalize(new BigDecimal(text.trim()));
    }

    /**
     * Tells whether a value lies in {@code [0.00, 10.00]}.
     *
     * @param value a normalized CGPA
     * @return {@code true} when legal
     */
    public static boolean inRange(BigDecimal value) {
        return value.compareTo(MIN) >= 0 && value.compareTo(MAX) <= 0;
    }
}
