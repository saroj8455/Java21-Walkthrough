package com.walkthrough.api.transaction.importing;

import com.walkthrough.api.transaction.dto.ImportResultResponse;
import com.walkthrough.api.transaction.dto.ImportSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Upload endpoints. Express + multer equivalent:
 * <pre>
 *   router.post('/api/v1/transactions/imports', upload.single('file'), handler)
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/transactions/imports")
@Tag(name = "Statement imports", description = "Upload PhonePe CSV statements and download rejected rows")
public class TransactionImportController {

    private final TransactionImportService importService;
    private final InvalidCsvExporter invalidCsvExporter;

    public TransactionImportController(TransactionImportService importService, InvalidCsvExporter invalidCsvExporter) {
        this.importService = importService;
        this.invalidCsvExporter = invalidCsvExporter;
    }

    /**
     * {@code multipart/form-data} with a part named "file". Returns 201 Created + Location header.
     * Possible errors: 400 (bad file), 413 (file > 5 MB), 429 (too many concurrent uploads), 500 (rolled back).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a PhonePe statement CSV",
            description = "Valid rows are saved in one transaction; duplicates are skipped; invalid rows are "
                    + "returned in a preview and downloadable as invalid_csv_statements.csv.")
    public ResponseEntity<ImportResultResponse> upload(@RequestPart("file") MultipartFile file) {
        ImportResultResponse result = importService.importStatement(file);
        return ResponseEntity
                .created(URI.create("/api/v1/transactions/imports/" + result.importId()))
                .body(result);
    }

    @GetMapping
    @Operation(summary = "Recent imports (latest 20)")
    public List<ImportSummaryResponse> recentImports() {
        return importService.recentImports();
    }

    @GetMapping("/{importId}")
    @Operation(summary = "One import")
    public ImportSummaryResponse getImport(@PathVariable long importId) {
        return importService.getImport(importId);
    }

    /** {@code Content-Disposition: attachment} makes the browser save the file instead of showing it. */
    @GetMapping("/{importId}/invalid-csv")
    @Operation(summary = "Download rejected rows as invalid_csv_statements.csv")
    public ResponseEntity<byte[]> downloadInvalidRows(@PathVariable long importId) {
        byte[] csv = invalidCsvExporter.export(importId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(InvalidCsvExporter.FILE_NAME).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
