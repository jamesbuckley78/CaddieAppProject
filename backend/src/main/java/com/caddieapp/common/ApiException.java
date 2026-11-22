package com.caddieapp.common;

import org.springframework.http.HttpStatus;

/** Throw this from a service to send a specific HTTP status and a message the app can show. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
