package com.placewise.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ConsoleIOTest {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal TEN = BigDecimal.TEN;

    @Test
    void integerRepromptsOnTextBlankAndOutOfRangeThenAccepts() {
        ScriptedConsole console = new ScriptedConsole("abc", "", "  ", "0", "11", "-5", "3.5", "7");
        assertThat(console.io.integer("Pick", 1, 10)).isEqualTo(7);
        String out = console.output();
        assertThat(out).contains("'abc' is not a whole number.").contains("Please type a number.")
                .contains("Please enter a number in the range 1-10.").contains("'3.5' is not a whole number.");
    }

    @Test
    void integerDefaultIsUsedOnBlankButStillValidatesTypedValues() {
        assertThat(new ScriptedConsole("").io.integer("Pick", 1, 10, 4)).isEqualTo(4);
        assertThat(new ScriptedConsole("99", "5").io.integer("Pick", 1, 10, 4)).isEqualTo(5);
    }

    @Test
    void integerBoundariesAreInclusive() {
        assertThat(new ScriptedConsole("1").io.integer("Pick", 1, 10)).isEqualTo(1);
        assertThat(new ScriptedConsole("10").io.integer("Pick", 1, 10)).isEqualTo(10);
    }

    @Test
    void hugeNumbersAreRejectedNotCrashed() {
        assertThat(new ScriptedConsole("99999999999999999999", "2").io.integer("Pick", 1, 10)).isEqualTo(2);
    }

    @Test
    void decimalRepromptsAndKeepsBoundaryValues() {
        ScriptedConsole console = new ScriptedConsole("x", "", "-0.01", "10.01", "NaN", "1e999999999", "10.00");
        assertThat(console.io.decimal("CGPA", ZERO, TEN)).isEqualTo(new BigDecimal("10.00"));
        assertThat(console.output()).contains("'x' is not a number.").contains("between 0 and 10")
                .contains("too large or too precise");
        assertThat(new ScriptedConsole("0").io.decimal("CGPA", ZERO, TEN)).isEqualByComparingTo(ZERO);
    }

    @Test
    void decimalDefaultAndOptionalVariants() {
        assertThat(new ScriptedConsole("").io.decimal("CGPA", ZERO, TEN, new BigDecimal("7.25")))
                .isEqualTo(new BigDecimal("7.25"));
        assertThat(new ScriptedConsole("8.5").io.decimal("CGPA", ZERO, TEN, new BigDecimal("7.25")))
                .isEqualByComparingTo("8.5");
        assertThat(new ScriptedConsole("").io.optionalDecimal("Min", ZERO, TEN)).isEmpty();
        assertThat(new ScriptedConsole("abc", "6.5").io.optionalDecimal("Min", ZERO, TEN)).contains(new BigDecimal("6.5"));
        assertThat(new ScriptedConsole("11", "").io.optionalDecimal("Min", ZERO, TEN)).isEmpty();
    }

    @Test
    void textRequiresContentOrUsesTheDefault() {
        assertThat(new ScriptedConsole("", "  ", " Asha ").io.text("Name")).isEqualTo("Asha");
        assertThat(new ScriptedConsole("").io.text("Name", "Keep")).isEqualTo("Keep");
        assertThat(new ScriptedConsole(" New ").io.text("Name", "Keep")).isEqualTo("New");
        assertThat(new ScriptedConsole("").io.text("Email", "")).isEmpty();
    }

    @Test
    void confirmAcceptsYesNoInAnyCaseAndRepromptsOtherwise() {
        assertThat(new ScriptedConsole("maybe", "", "Y").io.confirm("Sure")).isTrue();
        assertThat(new ScriptedConsole("NO").io.confirm("Sure")).isFalse();
        assertThat(new ScriptedConsole("yes").io.confirm("Sure")).isTrue();
        assertThat(new ScriptedConsole("n").io.confirm("Sure")).isFalse();
        ScriptedConsole console = new ScriptedConsole("what", "n");
        console.io.confirm("Sure");
        assertThat(console.output()).contains("Please answer y or n.");
    }

    @Test
    void numberReadsLongsWithDefault() {
        assertThat(new ScriptedConsole("").io.number("Seed", 42)).isEqualTo(42);
        assertThat(new ScriptedConsole("x", "9000000000").io.number("Seed", 42)).isEqualTo(9_000_000_000L);
    }

    @Test
    void pathUsesTheDefaultOnBlank() {
        assertThat(new ScriptedConsole("").io.path("File", Path.of("a", "b.csv"))).isEqualTo(Path.of("a", "b.csv"));
        assertThat(new ScriptedConsole("other.csv").io.path("File", Path.of("a"))).isEqualTo(Path.of("other.csv"));
    }

    @Test
    void endOfInputIsReportedAsInputClosedNotAsAHangOrCrash() {
        assertThatThrownBy(() -> new ScriptedConsole().io.integer("Pick", 1, 10)).isInstanceOf(InputClosedException.class);
        assertThatThrownBy(() -> new ScriptedConsole("abc").io.integer("Pick", 1, 10))
                .isInstanceOf(InputClosedException.class); // one bad answer, then the script ends
        assertThatThrownBy(() -> new ScriptedConsole().io.line("x")).isInstanceOf(InputClosedException.class)
                .hasMessage("Input ended");
    }

    @Test
    void printfAndBlankWriteToTheInjectedStream() {
        ScriptedConsole console = new ScriptedConsole();
        console.io.printf("%-4s|%3d", "ab", 7);
        console.io.blank();
        console.io.println("done");
        assertThat(console.output()).isEqualTo("ab  |  7" + System.lineSeparator() + System.lineSeparator()
                + "done" + System.lineSeparator());
        assertThat(console.io.out()).isNotNull();
    }
}
