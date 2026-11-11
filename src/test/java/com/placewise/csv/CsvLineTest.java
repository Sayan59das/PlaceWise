package com.placewise.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CsvLineTest {

    @Test
    void splitsPlainFields() {
        assertThat(CsvLine.parse("a,b,c")).containsExactly("a", "b", "c");
    }

    @Test
    void keepsEmptyFieldsIncludingTrailingOnes() {
        assertThat(CsvLine.parse("a,,c,")).containsExactly("a", "", "c", "");
        assertThat(CsvLine.parse(",")).containsExactly("", "");
        assertThat(CsvLine.parse("")).containsExactly("");
    }

    @Test
    void aQuotedFieldMayContainCommas() {
        assertThat(CsvLine.parse("R1,\"Rao, Asha\",CSE")).containsExactly("R1", "Rao, Asha", "CSE");
    }

    @Test
    void doubledQuotesBecomeOneQuote() {
        assertThat(CsvLine.parse("\"say \"\"hi\"\"\",x")).containsExactly("say \"hi\"", "x");
    }

    @Test
    void quotedEmptyFieldIsEmpty() {
        assertThat(CsvLine.parse("\"\",x")).containsExactly("", "x");
    }

    @Test
    void aQuoteInTheMiddleOfAnUnquotedFieldIsLiteral() {
        assertThat(CsvLine.parse("ab\"cd,e")).containsExactly("ab\"cd", "e");
    }

    @Test
    void anUnclosedQuoteIsAnError() {
        assertThatThrownBy(() -> CsvLine.parse("a,\"b,c")).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not closed");
    }

    @ParameterizedTest
    // whitespace must be kept: it is exactly what two of these cases are about
    @CsvSource(delimiter = '|', ignoreLeadingAndTrailingWhitespace = false,
            value = {"plain|plain", "has,comma|\"has,comma\"", "has\"quote|\"has\"\"quote\"",
                    " lead|\" lead\"", "trail |\"trail \""})
    void escapeQuotesOnlyWhenNeeded(String raw, String expected) {
        assertThat(CsvLine.escape(raw)).isEqualTo(expected);
    }

    @Test
    void nullAndEmptyEscapeToNothing() {
        assertThat(CsvLine.escape(null)).isEmpty();
        assertThat(CsvLine.escape("")).isEmpty();
    }

    @Test
    void formatThenParseGivesBackTheSameFieldsForAwkwardRandomText() {
        Random random = new Random(5);
        String alphabet = "ab ,\"\t;x1é";
        for (int round = 0; round < 2_000; round++) {
            List<String> fields = new ArrayList<>();
            int count = 1 + random.nextInt(5);
            for (int f = 0; f < count; f++) {
                StringBuilder text = new StringBuilder();
                int length = random.nextInt(8);
                for (int c = 0; c < length; c++) {
                    text.append(alphabet.charAt(random.nextInt(alphabet.length())));
                }
                fields.add(text.toString());
            }
            assertThat(CsvLine.parse(CsvLine.format(fields))).as("fields %s", fields).isEqualTo(fields);
        }
    }
}
