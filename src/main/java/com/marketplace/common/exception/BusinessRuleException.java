
package com.marketplace.common.exception;

public class BusinessRuleException extends MarketplaceException {

    public BusinessRuleException(String message) {
        this(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }

    protected BusinessRuleException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
