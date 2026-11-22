package com.walkthrough.api.transaction.importing;

import java.util.List;

/**
 * Result of parsing ONE data line: either {@link Valid} or {@link Invalid} - nothing else.
 *
 * <p><b>Sealed interface</b> (Java 17): {@code permits} lists every allowed implementation. The compiler
 * therefore knows the full set of cases, so a {@code switch} over a {@code ParsedRow} needs no
 * {@code default} branch and fails to compile if a new case is added but not handled.
 *
 * <p>TypeScript equivalent - a discriminated union:
 * <pre>
 *   type ParsedRow = { kind: 'valid', lineNumber: number, row: StatementRow }
 *                  | { kind: 'invalid', lineNumber: number, cells: string[], reason: string };
 * </pre>
 */
public sealed interface ParsedRow permits ParsedRow.Valid, ParsedRow.Invalid {

    /** 1-based line number in the uploaded file (so users can find it in Excel). */
    int lineNumber();

    record Valid(int lineNumber, StatementRow row) implements ParsedRow {
    }

    /**
     * @param cells  the original cells, kept so they can be written to invalid_csv_statements.csv
     * @param reason human-readable explanation, several problems joined with "; "
     */
    record Invalid(int lineNumber, List<String> cells, String reason) implements ParsedRow {
        public Invalid {
            cells = List.copyOf(cells);   // defensive copy -> the record stays immutable
        }
    }
}
