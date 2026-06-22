package com.marketplace.commerce.api.exception;

import com.marketplace.commerce.application.dto.ApiResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        ApiResponseDto<Object> body = ApiResponseDto.error("Validation failed", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        ApiResponseDto<Object> body = ApiResponseDto.error(ex.getMessage(), null, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleNotFound(
            java.util.NoSuchElementException ex, HttpServletRequest request) {
        ApiResponseDto<Object> body = ApiResponseDto.error(ex.getMessage(), null, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        ApiResponseDto<Object> body = ApiResponseDto.error(ex.getMessage(), null, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(org.springframework.web.client.RestClientException.class)
    public ResponseEntity<ApiResponseDto<Object>> handleExternalServiceError(
            org.springframework.web.client.RestClientException ex, HttpServletRequest request) {
        log.error("External service call failed on {}: {}", request.getRequestURI(), ex.getMessage());
        ApiResponseDto<Object> body = ApiResponseDto.error(
                "Payment service temporarily unavailable. Please try again.",
                null, request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponseDto<Object>> handleGeneric(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        ApiResponseDto<Object> body = ApiResponseDto.error(
                "Internal server error", List.of(ex.getMessage()), request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}