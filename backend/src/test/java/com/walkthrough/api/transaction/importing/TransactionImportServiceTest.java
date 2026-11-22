package com.walkthrough.api.transaction.importing;

import com.walkthrough.api.transaction.dto.ImportResultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Real parser + real limiter; the DB-facing writer and repository are mocked. */
@ExtendWith(MockitoExtension.class)
class TransactionImportServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final String CSV = """
            Date,Time,Transaction Details,Transaction ID,UTR,Transaction Type,Credit/debit instrument,Amount
            2022-04-04,00:29,Paid to Shop,T0000000000000000000001,1,Debit,XXXXXX0001,10.00
            not,a,valid,row
            """;

    @Mock
    private TransactionImportWriter writer;

    @Mock
    private TransactionImportRepository importRepository;

    private TransactionImportService service;

    @BeforeEach
    void setUp() {
        ImportProperties properties = new ImportProperties(2, Duration.ofMillis(100), 500, 1000);
        service = new TransactionImportService(new PhonePeStatementParser(), writer,
                new ImportConcurrencyLimiter(properties), importRepository, properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void importsValidRowsAndReturnsInvalidPreview() {
        TransactionImport saved = TransactionImport.started("statement.csv", NOW);
        saved.complete(2, 1, 0, 1, NOW.plusMillis(42));
        ReflectionTestUtils.setField(saved, "id", 7L);     // simulate the DB-generated id
        when(writer.write(eq("statement.csv"), any(), eq(NOW))).thenReturn(saved);

        ImportResultResponse response = service.importStatement(csv("statement.csv"));

        ArgumentCaptor<StatementParseResult> parsed = ArgumentCaptor.forClass(StatementParseResult.class);
        verify(writer).write(eq("statement.csv"), parsed.capture(), eq(NOW));
        assertThat(parsed.getValue().validRows()).hasSize(1);
        assertThat(parsed.getValue().invalidRows()).hasSize(1);

        assertThat(response.importId()).isEqualTo(7);
        assertThat(response.durationMs()).isEqualTo(42);
        assertThat(response.invalidCsvUrl()).isEqualTo("/api/v1/transactions/imports/7/invalid-csv");
        assertThat(response.invalidRowsPreview()).singleElement()
                .satisfies(row -> assertThat(row.lineNumber()).isEqualTo(3));
    }

    @Test
    void recordsFailureAndThrowsWhenDatabaseFails() {
        when(writer.write(anyString(), any(), any())).thenThrow(new DataIntegrityViolationException("db said no"));
        TransactionImport failed = TransactionImport.failed("statement.csv", NOW, 2, 1, "db said no", NOW);
        ReflectionTestUtils.setField(failed, "id", 9L);
        when(writer.recordFailure(eq("statement.csv"), any(), eq(NOW), anyString())).thenReturn(failed);

        assertThatThrownBy(() -> service.importStatement(csv("statement.csv")))
                .isInstanceOf(ImportFailedException.class)
                .satisfies(ex -> assertThat(((ImportFailedException) ex).getImportId()).isEqualTo(9));
    }

    @Test
    void rejectsNonCsvAndEmptyFilesBeforeTouchingTheDatabase() {
        assertThatThrownBy(() -> service.importStatement(csv("statement.xlsx")))
                .isInstanceOf(InvalidUploadException.class);
        assertThatThrownBy(() -> service.importStatement(
                new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0])))
                .isInstanceOf(InvalidUploadException.class);

        verify(writer, never()).write(anyString(), any(), any());
    }

    @Test
    void stripsClientPathFromFileName() {
        TransactionImport saved = TransactionImport.started("statement.csv", NOW);
        saved.complete(2, 1, 0, 1, NOW);
        ReflectionTestUtils.setField(saved, "id", 1L);
        when(writer.write(eq("statement.csv"), any(), any())).thenReturn(saved);

        service.importStatement(csv("C:\\fakepath\\statement.csv"));

        verify(writer).write(eq("statement.csv"), any(), any());
    }

    private static MockMultipartFile csv(String name) {
        return new MockMultipartFile("file", name, "text/csv", CSV.getBytes(StandardCharsets.UTF_8));
    }
}
