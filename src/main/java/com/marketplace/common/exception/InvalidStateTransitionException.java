
package com.marketplace.common.exception;

public class InvalidStateTransitionException extends BusinessRuleException {

    public InvalidStateTransitionException(String message) {
        super(ErrorCode.INVALID_STATE_TRANSITION, message);
    }
}
