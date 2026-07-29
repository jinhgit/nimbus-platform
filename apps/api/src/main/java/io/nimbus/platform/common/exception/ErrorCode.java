package io.nimbus.platform.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    INTERNAL_ERROR("COMMON001", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR("COMMON002", "Validation failed", HttpStatus.BAD_REQUEST),
    NOT_FOUND("COMMON003", "Resource not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("COMMON004", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("COMMON005", "Forbidden", HttpStatus.FORBIDDEN);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus status;

    ErrorCode(String code, String defaultMessage, HttpStatus status) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
