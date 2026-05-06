package com.grid07.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a Redis guardrail (horizontal cap, vertical cap, or cooldown cap)
 * blocks an incoming request.  Maps to HTTP 429 Too Many Requests.
 */
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class GuardrailException extends RuntimeException {

    private final String guardrailType;

    public GuardrailException(String guardrailType, String message) {
        super(message);
        this.guardrailType = guardrailType;
    }

    public String getGuardrailType() {
        return guardrailType;
    }
}
