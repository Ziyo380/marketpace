package com.marketplace.common.exception;

import java.util.Objects;

public abstract class MarketplaceException extends RuntimeException {

    private final ErrorCode errorCode;

    protected MarketplaceException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
