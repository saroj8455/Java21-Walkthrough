package com.walkthrough.api.transaction.importing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Audit record of one uploaded file. Table {@code transaction_imports}.
 *
 * <p>Instead of public setters, the entity exposes <b>intention-revealing methods</b>
 * ({@link #started}, {@link #complete}, {@link #fail}). The rules for a valid state live here,
 * in one place - a small taste of Domain-Driven Design.
 */
@Entity
@Table(name = "transaction_imports")
public class TransactionImport {

    private static final int MAX_ERROR_MESSAGE = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ImportStatus status;

    @Column(name = "total_rows", nullable = false)
    private int totalRows;

    @Column(name = "imported_rows", nullable = false)
    private int importedRows;

    @Column(name = "duplicate_rows", nullable = false)
    private int duplicateRows;

    @Column(name = "invalid_rows", nullable = false)
    private int invalidRows;

    @Column(name = "error_message", length = MAX_ERROR_MESSAGE)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected TransactionImport() {
    }

    private TransactionImport(String fileName, ImportStatus status, Instant startedAt) {
        this.fileName = fileName;
        this.status = status;
        this.startedAt = startedAt;
    }

    /** Static factory method: a named constructor that reads better than {@code new ...}. */
    public static TransactionImport started(String fileName, Instant startedAt) {
        return new TransactionImport(fileName, ImportStatus.PROCESSING, startedAt);
    }

    public void complete(int totalRows, int importedRows, int duplicateRows, int invalidRows, Instant finishedAt) {
        this.status = ImportStatus.COMPLETED;
        this.totalRows = totalRows;
        this.importedRows = importedRows;
        this.duplicateRows = duplicateRows;
        this.invalidRows = invalidRows;
        this.finishedAt = finishedAt;
    }

    /** A failed import saved nothing, so imported/duplicate stay 0; totals show what the file contained. */
    public static TransactionImport failed(String fileName, Instant startedAt, int totalRows, int invalidRows,
                                           String errorMessage, Instant finishedAt) {
        TransactionImport failed = new TransactionImport(fileName, ImportStatus.FAILED, startedAt);
        failed.totalRows = totalRows;
        failed.invalidRows = invalidRows;
        failed.errorMessage = truncate(errorMessage);
        failed.finishedAt = finishedAt;
        return failed;
    }

    private static String truncate(String message) {
        if (message == null || message.length() <= MAX_ERROR_MESSAGE) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE);
    }

    public Long getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public ImportStatus getStatus() {
        return status;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public int getImportedRows() {
        return importedRows;
    }

    public int getDuplicateRows() {
        return duplicateRows;
    }

    public int getInvalidRows() {
        return invalidRows;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
