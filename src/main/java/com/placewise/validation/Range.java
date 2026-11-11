package com.placewise.validation;

/** A small helper used by both validators. */
public final class Range {

    private Range() {
    }

    /**
     * Tells whether {@code min <= value <= max}.
     *
     * <p>This is a generic method with a <em>bounded type</em>: {@code V extends Comparable<V>} means "any
     * type that can be compared with itself", so the same method works for an {@code Integer} semester
     * and for a {@code BigDecimal} CGPA.
     */
    public static <V extends Comparable<V>> boolean isBetween(V value, V min, V max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}
