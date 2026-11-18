package com.marketplace.common.exception;

public class ResourceNotFoundException extends MarketplaceException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        this("%s with identifier '%s' was not found".formatted(resourceName, identifier));
    }
}
