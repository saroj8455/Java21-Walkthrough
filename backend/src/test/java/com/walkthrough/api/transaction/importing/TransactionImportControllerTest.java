package com.walkthrough.api.transaction.importing;

import com.walkthrough.api.transaction.dto.ImportResultResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionImportController.class)
class TransactionImportControllerTest {

    private final MockMultipartFile file = new MockMultipartFile(
            "file", "statement.csv", "text/csv", "Date,...".getBytes(StandardCharsets.UTF_8));

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransactionImportService importService;

    @MockitoBean
    private InvalidCsvExporter invalidCsvExporter;

    @Test
    void uploadReturns201WithLocation() throws Exception {
        when(importService.importStatement(any())).thenReturn(new ImportResultResponse(
                5, "statement.csv", ImportStatus.COMPLETED, 10, 8, 1, 1, 30, Instant.parse("2026-01-15T10:00:00Z"),
                "/api/v1/transactions/imports/5/invalid-csv", List.of()));

        mockMvc.perform(multipart("/api/v1/transactions/imports").file(file))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/transactions/imports/5"))
                .andExpect(jsonPath("$.importedRows").value(8))
                .andExpect(jsonPath("$.invalidCsvUrl").value("/api/v1/transactions/imports/5/invalid-csv"));
    }

    @Test
    void busyServerReturns429WithRetryAfter() throws Exception {
        when(importService.importStatement(any())).thenThrow(new TooManyImportsException(Duration.ofSeconds(5)));

        mockMvc.perform(multipart("/api/v1/transactions/imports").file(file))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "5"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(5));
    }

    @Test
    void badFileReturns400() throws Exception {
        when(importService.importStatement(any())).thenThrow(new InvalidUploadException("Only .csv files are supported"));

        mockMvc.perform(multipart("/api/v1/transactions/imports").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Only .csv files are supported"));
    }

    @Test
    void missingFilePartReturns400() throws Exception {
        mockMvc.perform(multipart("/api/v1/transactions/imports"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void downloadsInvalidRowsAsAttachment() throws Exception {
        when(invalidCsvExporter.export(5)).thenReturn("Date,...\r\n".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(get("/api/v1/transactions/imports/5/invalid-csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"invalid_csv_statements.csv\""))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }
}
