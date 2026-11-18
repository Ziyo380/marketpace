package com.marketplace.auth.exception;

import com.marketplace.common.exception.ErrorCode;
import com.marketplace.common.exception.MarketplaceException;

public class InvalidCredentialsException extends MarketplaceException {
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, "Invalid username or password");
    }

}
