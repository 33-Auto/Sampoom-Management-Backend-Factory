package com.sampoom.factory.common.exception;

import com.sampoom.factory.common.response.ErrorStatus;
import org.springframework.http.HttpStatus;

public class UnauthorizedException extends BaseException {
    public UnauthorizedException() {
        super(HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }

    public UnauthorizedException(ErrorStatus errorStatus) {
        super(errorStatus.getHttpStatus(), errorStatus.getMessage(), errorStatus.getCode());
    }
}
