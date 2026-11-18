
package com.marketplace.common.exception;

public class ConflictException extends BusinessRuleException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
