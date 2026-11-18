
package com.marketplace.common.response;

import com.marketplace.common.exception.ErrorCode;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldValidationError> validationErrors
) {
    public ErrorResponse {
        validationErrors = validationErrors == null ? List.of() : List.copyOf(validationErrors);
    }

    public static ErrorResponse of(
            Instant timestamp,
            HttpStatus status,
            ErrorCode code,
            String message,
            String path,
            List<FieldValidationError> validationErrors
    ) {
        return new ErrorResponse(
                timestamp,
                status.value(),
                code.name(),
                message,
                path,
                validationErrors
        );
    }
}
