package com.structure.payment.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private int     status;
    private String  message;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    private String errorCode;
    private String path;
    private T      data;

    private List<FieldError> errors;

    public static <T> ApiResponse<T> success(T data, String message, HttpStatus httpStatus, String path) {
        return ApiResponse.<T>builder()
                .success(true)
                .status(httpStatus.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, HttpStatus httpStatus, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(httpStatus.value())
                .message(message)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }
    public static <T> ApiResponse<T> validationError(List<FieldError> fieldErrors, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .message("Validation failed")
                .errorCode("VALIDATION_ERROR")
                .path(path)
                .errors(fieldErrors)
                .timestamp(LocalDateTime.now())
                .build();
    }
    public static <T> ApiResponse<T> validationError(List<FieldError> errors, HttpStatus httpStatus, String path) {
        return ApiResponse.<T>builder()
                .success(false)
                .status(httpStatus.value())
                .message("Validation failed")
                .timestamp(LocalDateTime.now())
                .path(path)
                .errors(errors)
                .build();
    }
}
