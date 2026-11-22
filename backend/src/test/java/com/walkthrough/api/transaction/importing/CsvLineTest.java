package com.walkthrough.api.transaction.importing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvLineTest {

    @Test
    void splitsPlainAndQuotedCells() {
        assertThat(CsvLine.parse("a,\"b,c\",\"say \"\"hi\"\"\",,"))
                .containsExactly("a", "b,c", "say \"hi\"", "", "");
    }

    @Test
    void rejectsUnclosedQuote() {
        assertThatThrownBy(() -> CsvLine.parse("a,\"broken"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** @ParameterizedTest runs the same test once per value - like jest's test.each. */
    @ParameterizedTest
    @ValueSource(strings = {"plain", "with,comma", "with \"quote\"", ""})
    void formatAndParseRoundTrip(String cell) {
        List<String> cells = List.of(cell, "x");
        assertThat(CsvLine.parse(CsvLine.format(cells))).isEqualTo(cells);
    }
}
