package com.grid07.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Standardised error envelope returned by the global exception handler.
 *
 * <p>Using a consistent error schema allows API consumers to reliably parse
 * error details regardless of which exception was raised.</p>
 */
@Data
@Builder
public class ApiErrorResponse {

    /** HTTP status code (mirrored in the body for convenience). */
    private int status;

    /** Short machine-readable error category (e.g. "RATE_LIMITED", "NOT_FOUND"). */
    private String error;

    /** Human-readable message. */
    private String message;

    /** Individual field validation errors, if applicable. */
    private List<String> fieldErrors;

    /** Server-side timestamp of the error. */
    @Builder.Default
    private Instant timestamp = Instant.now();
}
