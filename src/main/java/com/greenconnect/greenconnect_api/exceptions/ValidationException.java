package com.greenconnect.greenconnect_api.exceptions;

public class ValidationException extends AppException {
    public ValidationException(ErrorCode errorCode) {
        super(errorCode);
    }
}