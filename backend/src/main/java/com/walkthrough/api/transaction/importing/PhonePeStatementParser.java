package com.walkthrough.api.transaction.importing;

import com.walkthrough.api.transaction.TransactionType;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Parses a PhonePe "Transaction Statement" CSV export.
 *
 * <p>File layout (real exports):
 * <pre>
 *   Transaction Statement for +91XXXXXXXXXX      <- preamble: skipped, NEVER stored (personal data)
 *   Duration,01 Apr 2022 - 31 Mar 2023           <- preamble
 *                                                <- blank lines are ignored
 *   Date,Time,Transaction Details,Transaction ID,UTR,Transaction Type,Credit/debit instrument,Amount
 *   2022-04-04,\t00:29,Paid to UDEMY,T2204...,2094...,Debit,XXXXXX0164,560.00
 *   ...
 *   "Do not fall prey to fictitious offers ..."  <- footer disclaimers -> reported as invalid rows
 * </pre>
 *
 * <p>Design: the parser is <b>pure</b> - no database, no Spring web types. It takes a {@link Reader}
 * and returns data, so it is trivial to unit test with a text block. It reads line by line
 * (streaming), so memory use is bounded by {@code maxRows}, not by file size.
 *
 * <p>Re-upload support: {@code invalid_csv_statements.csv} has the same 8 columns plus
 * "Line Number" and "Error Reason". The header check only looks at the first 8 columns and rows must have
 * as many cells as the header, so a corrected invalid file can be uploaded again as-is.
 */
@Component
public class PhonePeStatementParser {

    /** {@code List.of} (Java 9) creates an immutable list. */
    public static final List<String> EXPECTED_HEADER = List.of(
            "Date", "Time", "Transaction Details", "Transaction ID",
            "UTR", "Transaction Type", "Credit/debit instrument", "Amount");

    private static final int COLUMNS = EXPECTED_HEADER.size();
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm[:ss]");
    /** Compile regexes once (static final) - compiling them per row would be wasteful. */
    private static final Pattern TRANSACTION_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,12}(\\.\\d{1,2})?");
    private static final char BOM = '﻿';

    /**
     * @param reader  file content; the CALLER owns it and closes it (try-with-resources)
     * @param maxRows upper bound on data lines, protects the server from huge uploads
     * @return one {@link ParsedRow} per data line after the header, in file order
     * @throws InvalidUploadException if no header row is found or the file is too large
     */
    public List<ParsedRow> parse(Reader reader, int maxRows) throws IOException {
        var lines = new BufferedReader(reader);   // Java 10 `var`: type is obvious from the right side
        List<ParsedRow> rows = new ArrayList<>();
        Map<String, Integer> firstLineById = new HashMap<>();   // duplicate detection inside the file
        int headerColumns = -1;                                  // -1 = header not found yet
        int lineNumber = 0;
        String line;

        while ((line = lines.readLine()) != null) {
            lineNumber++;
            if (lineNumber == 1 && !line.isEmpty() && line.charAt(0) == BOM) {
                line = line.substring(1);                        // Excel adds a UTF-8 BOM
            }
            if (line.isBlank()) {                                // Java 11 String.isBlank()
                continue;
            }

            List<String> cells;
            try {
                cells = CsvLine.parse(line);
            } catch (IllegalArgumentException malformed) {
                if (headerColumns > 0) {
                    rows.add(new ParsedRow.Invalid(lineNumber, List.of(line), malformed.getMessage()));
                }
                continue;
            }

            if (headerColumns < 0) {                             // still in the preamble
                if (isHeader(cells)) {
                    headerColumns = cells.size();
                }
                continue;
            }

            if (rows.size() >= maxRows) {
                throw new InvalidUploadException("File has more than %,d data rows; split it into smaller files."
                        .formatted(maxRows));
            }
            rows.add(parseRow(lineNumber, cells, headerColumns, firstLineById));
        }

        if (headerColumns < 0) {
            throw new InvalidUploadException(
                    "Not a PhonePe statement: header row not found. Expected columns: "
                            + String.join(", ", EXPECTED_HEADER));
        }
        return rows;
    }

    private static boolean isHeader(List<String> cells) {
        if (cells.size() < COLUMNS) {
            return false;
        }
        for (int i = 0; i < COLUMNS; i++) {
            if (!cells.get(i).strip().equalsIgnoreCase(EXPECTED_HEADER.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates one line and collects EVERY problem (not just the first) so users can fix a row in one go.
     */
    ParsedRow parseRow(int lineNumber, List<String> cells, int expectedColumns, Map<String, Integer> firstLineById) {
        if (cells.size() != expectedColumns) {
            return new ParsedRow.Invalid(lineNumber, cells,
                    "Not a transaction row: expected %d columns but found %d".formatted(expectedColumns, cells.size()));
        }
        // Only the 8 statement columns matter; extra columns (re-uploaded error file) are ignored.
        List<String> data = cells.subList(0, COLUMNS);
        List<String> errors = new ArrayList<>();

        LocalDate date = parseDate(data.get(0), errors);
        LocalTime time = parseTime(data.get(1), errors);
        String details = required(data.get(2), "Transaction Details", 255, errors);
        String transactionId = parseTransactionId(data.get(3), errors);
        String utr = optional(data.get(4), "UTR", 64, errors);
        TransactionType type = TransactionType.fromStatementValue(data.get(5))
                .orElseGet(() -> {                                // lambda runs only when the Optional is empty
                    errors.add("Transaction Type must be Debit or Credit but was '%s'".formatted(data.get(5).strip()));
                    return null;
                });
        String instrument = optional(data.get(6), "Credit/debit instrument", 32, errors);
        BigDecimal amount = parseAmount(data.get(7), errors);

        if (transactionId != null) {
            // putIfAbsent returns the PREVIOUS value, or null if the key was new.
            Integer firstLine = firstLineById.putIfAbsent(transactionId, lineNumber);
            if (firstLine != null) {
                errors.add("Duplicate Transaction ID in this file (first seen on line %d)".formatted(firstLine));
            }
        }

        if (!errors.isEmpty()) {
            return new ParsedRow.Invalid(lineNumber, data, String.join("; ", errors));
        }
        return new ParsedRow.Valid(lineNumber,
                new StatementRow(transactionId, LocalDateTime.of(date, time), details, utr, type, instrument, amount));
    }

    // ---------------------------------------------------------------- field validators
    // Each returns the parsed value, or null after adding a message to `errors`.

    private static LocalDate parseDate(String raw, List<String> errors) {
        String value = raw.strip();
        try {
            return LocalDate.parse(value);                       // ISO yyyy-MM-dd
        } catch (DateTimeParseException ex) {
            errors.add("Date must be yyyy-MM-dd but was '%s'".formatted(value));
            return null;
        }
    }

    private static LocalTime parseTime(String raw, List<String> errors) {
        String value = raw.strip();                              // strip() also removes the leading TAB PhonePe adds
        try {
            return LocalTime.parse(value, TIME_FORMAT);
        } catch (DateTimeParseException ex) {
            errors.add("Time must be HH:mm but was '%s'".formatted(value));
            return null;
        }
    }

    private static String parseTransactionId(String raw, List<String> errors) {
        String value = raw.strip();
        if (value.isEmpty()) {
            errors.add("Transaction ID is required");
            return null;
        }
        if (!TRANSACTION_ID.matcher(value).matches()) {
            errors.add("Transaction ID must be 1-64 letters/digits but was '%s'".formatted(value));
            return null;
        }
        return value;
    }

    private static BigDecimal parseAmount(String raw, List<String> errors) {
        String value = raw.strip().replace(",", "");             // allow 1,234.50
        if (!AMOUNT.matcher(value).matches()) {
            errors.add("Amount must be a positive number with up to 2 decimals but was '%s'".formatted(raw.strip()));
            return null;
        }
        BigDecimal amount = new BigDecimal(value);
        if (amount.signum() <= 0) {
            errors.add("Amount must be greater than 0");
            return null;
        }
        return amount;
    }

    private static String required(String raw, String column, int maxLength, List<String> errors) {
        String value = raw.strip();
        if (value.isEmpty()) {
            errors.add(column + " is required");
            return null;
        }
        return checkLength(value, column, maxLength, errors);
    }

    /** Optional column: blank becomes null (SQL NULL). */
    private static String optional(String raw, String column, int maxLength, List<String> errors) {
        String value = raw.strip();
        return value.isEmpty() ? null : checkLength(value, column, maxLength, errors);
    }

    private static String checkLength(String value, String column, int maxLength, List<String> errors) {
        if (value.length() > maxLength) {
            errors.add("%s is longer than %d characters".formatted(column, maxLength));
            return null;
        }
        return value;
    }

    /** Header used for invalid_csv_statements.csv. */
    public static List<String> invalidCsvHeader() {
        List<String> header = new ArrayList<>(EXPECTED_HEADER);
        header.add("Line Number");
        header.add("Error Reason");
        return List.copyOf(header);
    }

    /** Pads short rows (e.g. a 1-cell footer line) so every line in the export lines up under the header. */
    static List<String> padToStatementColumns(List<String> cells) {
        if (cells.size() >= COLUMNS) {
            return cells;
        }
        List<String> padded = new ArrayList<>(cells);
        while (padded.size() < COLUMNS) {
            padded.add("");
        }
        return padded;
    }
}
