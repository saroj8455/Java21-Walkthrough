package com.walkthrough.api.transaction.importing;

import com.walkthrough.api.common.BadRequestException;

/** The uploaded FILE as a whole is unusable (empty, not CSV, no header, too many rows) -> HTTP 400. */
public class InvalidUploadException extends BadRequestException {

    public InvalidUploadException(String message) {
        super(message);
    }
}
