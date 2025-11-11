package com.sampoom.factory.common.exception;

import com.sampoom.factory.common.response.ErrorStatus;
import org.springframework.security.core.AuthenticationException;

public class CustomAuthenticationException extends AuthenticationException {
    private final ErrorStatus errorStatus;

    public CustomAuthenticationException(ErrorStatus errorStatus) {
        super(errorStatus.getMessage());
        this.errorStatus = errorStatus;
    }

    public ErrorStatus getErrorStatus() {
        return errorStatus;
    }
}
