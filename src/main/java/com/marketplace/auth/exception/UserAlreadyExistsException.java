package com.marketplace.auth.exception;

import com.marketplace.common.exception.ConflictException;

public class UserAlreadyExistsException extends ConflictException {
    public UserAlreadyExistsException(String username) {super("An account with username '" + username + "' already exists");
    }
}
