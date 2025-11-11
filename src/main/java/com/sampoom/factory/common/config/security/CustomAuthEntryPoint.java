package com.sampoom.factory.common.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sampoom.factory.common.exception.CustomAuthenticationException;
import com.sampoom.factory.common.response.ApiResponse;
import com.sampoom.factory.common.response.ErrorStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        ErrorStatus error;
        if (authException.getCause() instanceof CustomAuthenticationException customEx) {
            error = customEx.getErrorStatus();
        } else if (authException instanceof CustomAuthenticationException customEx) {
            error = customEx.getErrorStatus();
        } else {
            error = ErrorStatus.INVALID_TOKEN;
        }

        ApiResponse<Void> body = ApiResponse.errorWithCode(
                error.getCode(),
                error.getMessage()
        );

        response.setStatus(error.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));

        log.warn("[Security] {} {} -> {}", request.getMethod(), request.getRequestURI(), error.getMessage());
    }
}
