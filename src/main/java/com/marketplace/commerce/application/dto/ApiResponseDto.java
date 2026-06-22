package com.marketplace.commerce.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponseDto<T> {

    private boolean success;
    private String message;
    private T data;
    private List<String> errors;
    private OffsetDateTime timestamp;
    private String path;

    public static <T> ApiResponseDto<T> success(T data, String message, String path) {
        return ApiResponseDto.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .errors(null)
                .timestamp(OffsetDateTime.now())
                .path(path)
                .build();
    }

    public static <T> ApiResponseDto<T> error(String message, List<String> errors, String path) {
        return ApiResponseDto.<T>builder()
                .success(false)
                .message(message)
                .data(null)
                .errors(errors)
                .timestamp(OffsetDateTime.now())
                .path(path)
                .build();
    }
}