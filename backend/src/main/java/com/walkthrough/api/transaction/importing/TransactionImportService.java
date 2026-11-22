package com.walkthrough.api.transaction.importing;

import com.walkthrough.api.common.ResourceNotFoundException;
import com.walkthrough.api.transaction.dto.ImportResultResponse;
import com.walkthrough.api.transaction.dto.ImportSummaryResponse;
import com.walkthrough.api.transaction.dto.InvalidRowResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Orchestrates a statement upload:
 * <pre>
 *   validate file  ->  wait for an import slot (bulkhead)  ->  parse (no DB)  ->  write (one DB transaction)
 *                                                                               \-> on DB error: rollback,
 *                                                                                   record FAILED, HTTP 500
 * </pre>
 *
 * <p><b>This class is deliberately NOT {@code @Transactional}.</b> Parsing happens before any DB
 * connection is taken, so a slow upload or a big file never holds a pooled connection while it is being
 * read. Only {@link TransactionImportWriter#write} touches the database, briefly, in one transaction.
 */
@Service
public class TransactionImportService {

    private static final Logger log = LoggerFactory.getLogger(TransactionImportService.class);
    private static final int PREVIEW_SIZE = 20;

    private final PhonePeStatementParser parser;
    private final TransactionImportWriter writer;
    private final ImportConcurrencyLimiter limiter;
    private final TransactionImportRepository importRepository;
    private final ImportProperties properties;
    private final Clock clock;

    public TransactionImportService(PhonePeStatementParser parser, TransactionImportWriter writer,
                                    ImportConcurrencyLimiter limiter, TransactionImportRepository importRepository,
                                    ImportProperties properties, Clock clock) {
        this.parser = parser;
        this.writer = writer;
        this.limiter = limiter;
        this.importRepository = importRepository;
        this.properties = properties;
        this.clock = clock;
    }

    public ImportResultResponse importStatement(MultipartFile file) {
        String fileName = validateAndCleanFileName(file);
        // The lambda is the "work" that runs while holding one of the N import permits.
        return limiter.withPermit(() -> runImport(file, fileName));
    }

    private ImportResultResponse runImport(MultipartFile file, String fileName) {
        Instant startedAt = clock.instant();
        StatementParseResult result = parse(file);
        log.info("Parsed '{}': {} valid, {} invalid rows", fileName,
                result.validRows().size(), result.invalidRows().size());

        TransactionImport saved;
        try {
            saved = writer.write(fileName, result, startedAt);
        } catch (DataAccessException ex) {
            // By the time we get here, Spring has ALREADY rolled back write()'s transaction.
            TransactionImport failed = writer.recordFailure(fileName, result, startedAt,
                    ex.getMostSpecificCause().getMessage());
            throw new ImportFailedException(failed.getId(), ex);
        }

        log.info("Import {} '{}' completed: {} imported, {} duplicates, {} invalid", saved.getId(), fileName,
                saved.getImportedRows(), saved.getDuplicateRows(), saved.getInvalidRows());
        return toResponse(saved, result);
    }

    private StatementParseResult parse(MultipartFile file) {
        // try-with-resources (Java 7): the reader is closed automatically, even on exceptions
        // - like a `finally { stream.close() }` you can't forget.
        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)) {
            return StatementParseResult.from(parser.parse(reader, properties.maxRows()));
        } catch (IOException ex) {
            throw new InvalidUploadException("Could not read the uploaded file: " + ex.getMessage());
        }
    }

    /** Rejects obviously wrong uploads early, and strips any client-supplied path from the name. */
    private static String validateAndCleanFileName(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("Please choose a non-empty CSV file.");
        }
        // Browsers may send "C:\fakepath\x.csv"; never trust a client path.
        String original = StringUtils.getFilename(StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "statement.csv" : file.getOriginalFilename()));
        String name = (original == null || original.isBlank()) ? "statement.csv" : original;
        if (!name.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            throw new InvalidUploadException("Only .csv files are supported, got '%s'.".formatted(name));
        }
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static ImportResultResponse toResponse(TransactionImport saved, StatementParseResult result) {
        List<InvalidRowResponse> preview = result.invalidRows().stream()
                .limit(PREVIEW_SIZE)
                .map(row -> new InvalidRowResponse(row.lineNumber(), row.reason(), CsvLine.format(row.cells())))
                .toList();
        return new ImportResultResponse(
                saved.getId(),
                saved.getFileName(),
                saved.getStatus(),
                saved.getTotalRows(),
                saved.getImportedRows(),
                saved.getDuplicateRows(),
                saved.getInvalidRows(),
                Duration.between(saved.getStartedAt(), saved.getFinishedAt()).toMillis(),
                saved.getStartedAt(),
                ImportSummaryResponse.invalidCsvUrl(saved.getId(), saved.getInvalidRows()),
                preview);
    }

    // ------------------------------------------------------------------ read side

    @Transactional(readOnly = true)
    public List<ImportSummaryResponse> recentImports() {
        return importRepository.findTop20ByOrderByStartedAtDesc().stream()
                .map(ImportSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ImportSummaryResponse getImport(long importId) {
        return importRepository.findById(importId)                 // Optional<TransactionImport>
                .map(ImportSummaryResponse::from)                  // transform if present
                .orElseThrow(() -> new ResourceNotFoundException("Import", importId));
    }
}
