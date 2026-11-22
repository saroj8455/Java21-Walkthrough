package com.walkthrough.api.transaction.importing;

/** The database rejected the import; everything was rolled back -> HTTP 500 with the failed import id. */
public class ImportFailedException extends RuntimeException {

    private final long importId;

    public ImportFailedException(long importId, Throwable cause) {
        super("Import " + importId + " failed", cause);   // keep the cause for the logs
        this.importId = importId;
    }

    public long getImportId() {
        return importId;
    }
}
