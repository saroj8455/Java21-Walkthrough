package com.walkthrough.api.transaction.importing;

import com.walkthrough.api.common.ResourceNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@code invalid_csv_statements.csv} for one import.
 *
 * <p>Columns: the 8 original statement columns + "Line Number" + "Error Reason".
 * The user can fix the rows in Excel and upload the same file again - the parser accepts it.
 */
@Component
public class InvalidCsvExporter {

    public static final String FILE_NAME = "invalid_csv_statements.csv";
    private static final String CRLF = "\r\n";            // RFC 4180 line ending, what Excel expects
    private static final char UTF8_BOM = '﻿';        // makes Excel detect UTF-8 correctly

    private final JdbcTemplate jdbcTemplate;
    private final TransactionImportRepository importRepository;

    public InvalidCsvExporter(JdbcTemplate jdbcTemplate, TransactionImportRepository importRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.importRepository = importRepository;
    }

    /** Local record used only inside this class - records can be declared anywhere. */
    private record ErrorRow(int lineNumber, String rawRecord, String reason) {
    }

    @Transactional(readOnly = true)
    public byte[] export(long importId) {
        if (!importRepository.existsById(importId)) {
            throw new ResourceNotFoundException("Import", importId);
        }

        List<ErrorRow> errors = jdbcTemplate.query("""
                        SELECT line_number, raw_record, reason
                        FROM transaction_import_errors
                        WHERE import_id = ?
                        ORDER BY line_number
                        """,
                (rs, rowNum) -> new ErrorRow(rs.getInt("line_number"), rs.getString("raw_record"), rs.getString("reason")),
                importId);

        StringBuilder csv = new StringBuilder().append(UTF8_BOM);
        csv.append(CsvLine.format(PhonePeStatementParser.invalidCsvHeader())).append(CRLF);
        for (ErrorRow error : errors) {
            List<String> cells = new ArrayList<>(
                    PhonePeStatementParser.padToStatementColumns(CsvLine.parse(error.rawRecord())));
            cells.add(String.valueOf(error.lineNumber()));
            cells.add(error.reason());
            csv.append(CsvLine.format(cells)).append(CRLF);
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }
}
