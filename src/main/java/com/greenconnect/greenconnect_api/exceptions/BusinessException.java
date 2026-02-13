package com.greenconnect.greenconnect_api.exceptions;

public class BusinessException extends AppException {
    public BusinessException(ErrorCode errorCode) {
        super(errorCode);
    }
}