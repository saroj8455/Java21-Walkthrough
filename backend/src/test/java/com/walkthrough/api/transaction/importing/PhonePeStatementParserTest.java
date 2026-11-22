package com.walkthrough.api.transaction.importing;

import com.walkthrough.api.transaction.TransactionType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Uses a SYNTHETIC statement (fake ids and names) written as a text block.
 * Never put real personal statements in test files - this repository is public.
 */
class PhonePeStatementParserTest {

    private final PhonePeStatementParser parser = new PhonePeStatementParser();

    private static final String STATEMENT = """
            Transaction Statement for +910000000000
            Duration,01 Apr 2022 - 31 Mar 2023

            Date,Time,Transaction Details,Transaction ID,UTR,Transaction Type,Credit/debit instrument,Amount
            2022-04-04,\t00:29,Paid to Online Course,T0000000000000000000001,100000000001,Debit,XXXXXX0001,560.00
            2022-04-05,\t09:15,"Received from Friend, Inc",T0000000000000000000002,100000000002,Credit,XXXXXX0001,"1,250.50"
            2022-13-01,\t25:99,Bad date and time,T0000000000000000000003,100000000003,Debit,XXXXXX0001,10.00
            2022-04-06,\t10:00,,T0000000000000000000004,,Refund,XXXXXX0001,-5
            2022-04-07,\t11:00,Duplicate of line 5,T0000000000000000000001,100000000005,Debit,XXXXXX0001,1.00
            "Do not fall prey to fictitious offers of winning prizes, money circulation schemes and cheap funds, etc."
            """;

    @Test
    void separatesValidAndInvalidRowsWithReasons() throws IOException {
        StatementParseResult result = StatementParseResult.from(parser.parse(new StringReader(STATEMENT), 1000));

        assertThat(result.totalRows()).isEqualTo(6);
        assertThat(result.validRows()).hasSize(2);

        StatementRow first = result.validRows().getFirst();     // Java 21 SequencedCollection
        assertThat(first.transactionId()).isEqualTo("T0000000000000000000001");
        assertThat(first.transactedAt()).isEqualTo(LocalDateTime.of(2022, 4, 4, 0, 29));
        assertThat(first.type()).isEqualTo(TransactionType.DEBIT);
        assertThat(first.amount()).isEqualByComparingTo("560.00");

        StatementRow second = result.validRows().get(1);
        assertThat(second.details()).isEqualTo("Received from Friend, Inc");   // quoted comma kept
        assertThat(second.amount()).isEqualByComparingTo(new BigDecimal("1250.50"));

        assertThat(result.invalidRows())
                .extracting(ParsedRow.Invalid::lineNumber)
                .containsExactly(7, 8, 9, 10);                  // real line numbers in the file

        assertThat(result.invalidRows().get(0).reason())
                .contains("Date must be yyyy-MM-dd").contains("Time must be HH:mm");
        assertThat(result.invalidRows().get(1).reason())
                .contains("Transaction Details is required")
                .contains("Transaction Type must be Debit or Credit")
                .contains("Amount must be a positive number");
        assertThat(result.invalidRows().get(2).reason())
                .isEqualTo("Duplicate Transaction ID in this file (first seen on line 5)");
        assertThat(result.invalidRows().get(3).reason()).startsWith("Not a transaction row");
    }

    @Test
    void acceptsReUploadedInvalidCsvWithExtraColumns() throws IOException {
        String fixed = """
                ﻿Date,Time,Transaction Details,Transaction ID,UTR,Transaction Type,Credit/debit instrument,Amount,Line Number,Error Reason
                2022-04-06,10:00,Fixed row,T0000000000000000000004,,Credit,XXXXXX0001,5.00,8,Amount must be ...
                """;
        StatementParseResult result = StatementParseResult.from(parser.parse(new StringReader(fixed), 1000));

        assertThat(result.invalidRows()).isEmpty();
        assertThat(result.validRows()).singleElement()
                .satisfies(row -> assertThat(row.type()).isEqualTo(TransactionType.CREDIT));
    }

    @Test
    void rejectsFilesWithoutHeader() {
        assertThatThrownBy(() -> parser.parse(new StringReader("just,some,csv\n1,2,3\n"), 1000))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("header row not found");
    }

    @Test
    void rejectsFilesAboveRowLimit() {
        assertThatThrownBy(() -> parser.parse(new StringReader(STATEMENT), 2))
                .isInstanceOf(InvalidUploadException.class)
                .hasMessageContaining("more than 2 data rows");
    }
}
