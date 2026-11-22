package com.walkthrough.api.transaction.dto;

import com.walkthrough.api.transaction.importing.ImportStatus;
import com.walkthrough.api.transaction.importing.TransactionImport;

import java.time.Instant;

/** One row of the import history. */
public record ImportSummaryResponse(
        long importId,
        String fileName,
        ImportStatus status,
        int totalRows,
        int importedRows,
        int duplicateRows,
        int invalidRows,
        String errorMessage,
        Instant startedAt,
        Instant finishedAt,
        String invalidCsvUrl
) {
    /** Mapping lives next to the DTO: entity -> DTO in one obvious place. */
    public static ImportSummaryResponse from(TransactionImport anImport) {
        return new ImportSummaryResponse(
                anImport.getId(),
                anImport.getFileName(),
                anImport.getStatus(),
                anImport.getTotalRows(),
                anImport.getImportedRows(),
                anImport.getDuplicateRows(),
                anImport.getInvalidRows(),
                anImport.getErrorMessage(),
                anImport.getStartedAt(),
                anImport.getFinishedAt(),
                invalidCsvUrl(anImport.getId(), anImport.getInvalidRows()));
    }

    /** Relative URL (works behind the nginx proxy too), or null when there is nothing to download. */
    public static String invalidCsvUrl(long importId, int invalidRows) {
        return invalidRows > 0 ? "/api/v1/transactions/imports/%d/invalid-csv".formatted(importId) : null;
    }
}
