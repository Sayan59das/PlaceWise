package com.placewise.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.placewise.model.ApplicationStatus;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void simpleValuesAreWrittenAsJson() {
        assertThat(Json.write(null)).isEqualTo("null");
        assertThat(Json.write("CU2501")).isEqualTo("\"CU2501\"");
        assertThat(Json.write(42)).isEqualTo("42");
        assertThat(Json.write(7L)).isEqualTo("7");
        assertThat(Json.write(true)).isEqualTo("true");
        assertThat(Json.write(false)).isEqualTo("false");
    }

    @Test
    void bigDecimalsKeepTheirDecimalsAndNeverUseExponents() {
        assertThat(Json.write(new BigDecimal("8.00"))).isEqualTo("8.00");
        assertThat(Json.write(new BigDecimal("1E+1"))).isEqualTo("10");
    }

    @Test
    void textIsEscapedSoItCannotBreakOutOfItsQuotes() {
        assertThat(Json.write("say \"hi\"")).isEqualTo("\"say \\\"hi\\\"\"");
        assertThat(Json.write("C:\\data\\rejects.csv")).isEqualTo("\"C:\\\\data\\\\rejects.csv\"");
        assertThat(Json.write("line1\nline2\ttab\r")).isEqualTo("\"line1\\nline2\\ttab\\r\"");
        assertThat(Json.write("bell\u0007")).isEqualTo("\"bell\\u0007\"");
        assertThat(Json.write("<script>")).isEqualTo("\"<script>\""); // HTML is escaped by the page, not here
    }

    @Test
    void listsAndMapsNestAndKeepTheirOrder() {
        Map<String, Object> student = new LinkedHashMap<>();
        student.put("rollNo", "CU2501");
        student.put("cgpa", new BigDecimal("9.42"));
        student.put("email", null);
        student.put("tags", List.of("a", "b"));

        assertThat(Json.write(student))
                .isEqualTo("{\"rollNo\":\"CU2501\",\"cgpa\":9.42,\"email\":null,\"tags\":[\"a\",\"b\"]}");
        assertThat(Json.write(List.of(1, List.of(2, 3)))).isEqualTo("[1,[2,3]]");
        assertThat(Json.write(new ArrayList<>())).isEqualTo("[]");
        assertThat(Json.write(Map.of())).isEqualTo("{}");
    }

    @Test
    void anythingElseIsWrittenAsItsText() {
        assertThat(Json.write(ApplicationStatus.WAITLISTED)).isEqualTo("\"WAITLISTED\"");
        assertThat(Json.write(Path.of("reports"))).isEqualTo("\"reports\"");
    }
}
