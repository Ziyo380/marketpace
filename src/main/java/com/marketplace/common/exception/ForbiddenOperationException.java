
package com.marketplace.common.exception;

public class ForbiddenOperationException extends MarketplaceException {

    public ForbiddenOperationException(String message) {
        super(ErrorCode.FORBIDDEN_OPERATION, message);
    }
}
