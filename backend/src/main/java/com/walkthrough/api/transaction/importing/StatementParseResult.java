package com.walkthrough.api.transaction.importing;

import java.util.ArrayList;
import java.util.List;

/** All data lines of one file, split into valid rows and rejected rows. */
public record StatementParseResult(List<StatementRow> validRows, List<ParsedRow.Invalid> invalidRows) {

    public StatementParseResult {
        validRows = List.copyOf(validRows);
        invalidRows = List.copyOf(invalidRows);
    }

    /**
     * Partitions parsed rows.
     *
     * <p>Java 21 <b>pattern matching for switch</b> with <b>record patterns</b>:
     * {@code case ParsedRow.Valid(int line, StatementRow row)} checks the type AND destructures the
     * record in one step (like {@code const { row } = parsed} after checking {@code parsed.kind}).
     * Because {@link ParsedRow} is sealed, no {@code default} branch is needed.
     */
    public static StatementParseResult from(List<ParsedRow> rows) {
        List<StatementRow> valid = new ArrayList<>();
        List<ParsedRow.Invalid> invalid = new ArrayList<>();
        for (ParsedRow parsed : rows) {
            switch (parsed) {
                case ParsedRow.Valid(int line, StatementRow row) -> valid.add(row);
                case ParsedRow.Invalid rejected -> invalid.add(rejected);
            }
        }
        return new StatementParseResult(valid, invalid);
    }

    /** Number of data lines in the file (header and preamble not counted). */
    public int totalRows() {
        return validRows.size() + invalidRows.size();
    }
}
