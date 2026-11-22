package com.walkthrough.api.transaction.dto;

import com.walkthrough.api.transaction.importing.ImportStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/** Response of {@code POST /api/v1/transactions/imports}. */
@Schema(description = "Outcome of one statement upload")
public record ImportResultResponse(
        long importId,
        String fileName,
        ImportStatus status,
        @Schema(description = "Data lines in the file (header/preamble excluded)") int totalRows,
        @Schema(description = "New transactions saved") int importedRows,
        @Schema(description = "Valid rows skipped because the Transaction ID already exists") int duplicateRows,
        @Schema(description = "Rejected rows, downloadable as invalid_csv_statements.csv") int invalidRows,
        long durationMs,
        Instant startedAt,
        @Schema(description = "Download link for rejected rows; null when there are none") String invalidCsvUrl,
        @Schema(description = "First rejected rows, for display") List<InvalidRowResponse> invalidRowsPreview
) {
}
