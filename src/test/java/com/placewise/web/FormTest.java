package com.placewise.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FormTest {

    @Test
    void namesAndValuesAreSplitAndDecoded() {
        Form form = Form.parse("rollNo=cu2501&name=Aarav+Sharma&branches=CSE%2C+IT&note=50%25");

        assertThat(form.text("rollNo")).isEqualTo("cu2501");
        assertThat(form.text("name")).isEqualTo("Aarav Sharma");
        assertThat(form.text("branches")).isEqualTo("CSE, IT");
        assertThat(form.text("note")).isEqualTo("50%");
    }

    @Test
    void aMissingOrEmptyFieldIsBlankText() {
        assertThat(Form.parse(null).text("anything")).isEmpty();
        assertThat(Form.parse("").text("anything")).isEmpty();
        assertThat(Form.parse("email=&flag").text("email")).isEmpty();
        assertThat(Form.parse("email=&flag").text("flag")).isEmpty();
        assertThat(Form.parse("name=++padded++").text("name")).isEqualTo("padded");
    }

    @Test
    void numbersAreParsed() {
        Form form = Form.parse("semester=6&seed=9000000000&cgpa=7.85");

        assertThat(form.integer("semester")).isEqualTo(6);
        assertThat(form.number("seed")).isEqualTo(9_000_000_000L);
        assertThat(form.decimal("cgpa")).isEqualByComparingTo("7.85");
    }

    @Test
    void blankFieldsFallBackToTheDefault() {
        Form form = Form.parse("page=&seed=");

        assertThat(form.integer("page", 1)).isEqualTo(1);
        assertThat(form.integer("pageSize", 10)).isEqualTo(10);
        assertThat(form.number("seed", 42L)).isEqualTo(42L);
        assertThat(form.decimal("minCgpa", null)).isNull();
        assertThat(form.decimal("cgpaIncrease", BigDecimal.ZERO)).isEqualByComparingTo("0");
    }

    @Test
    void aBlankRequiredNumberAsksForIt() {
        Form form = Form.parse("name=x");

        assertThatThrownBy(() -> form.integer("semester")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Please fill in semester");
        assertThatThrownBy(() -> form.number("id")).hasMessage("Please fill in id");
        assertThatThrownBy(() -> form.decimal("cgpa")).hasMessage("Please fill in cgpa");
    }

    @Test
    void textThatIsNotANumberIsNamedInTheMessage() {
        Form form = Form.parse("semester=abc&id=1.5&cgpa=eight");

        assertThatThrownBy(() -> form.integer("semester")).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'abc' is not a whole number (semester)");
        assertThatThrownBy(() -> form.number("id")).hasMessage("'1.5' is not a whole number (id)");
        assertThatThrownBy(() -> form.decimal("cgpa")).hasMessage("'eight' is not a number (cgpa)");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1e999999999", "12345678901", "0.000000000000000000001"})
    void absurdlyLargeOrPreciseDecimalsAreRefusedBeforeTheyCostMemory(String text) {
        Form form = Form.parse("cgpa=" + text);

        assertThatThrownBy(() -> form.decimal("cgpa")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("too large or too precise");
    }

    @Test
    void aBrokenEscapeIsRefused() {
        assertThatThrownBy(() -> Form.parse("name=%G1")).isInstanceOf(IllegalArgumentException.class);
    }
}
