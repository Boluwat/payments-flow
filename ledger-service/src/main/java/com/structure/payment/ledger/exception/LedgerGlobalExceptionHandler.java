package com.structure.payment.ledger.exception;

import com.structure.payment.common.dto.ApiResponse;
import com.structure.payment.common.exception.AccountNotFoundException;
import com.structure.payment.common.exception.DailyLimitExceededException;
import com.structure.payment.common.exception.HoldNotFoundException;
import com.structure.payment.common.exception.InsufficientFundsException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Slf4j
public class LedgerGlobalExceptionHandler {

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccountNotFoundException(
            AccountNotFoundException ex, HttpServletRequest request) {
        log.warn("Account not found: uri={} error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        HttpStatus.BAD_REQUEST,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(DailyLimitExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleDailyLimitExceededException(
            DailyLimitExceededException ex, HttpServletRequest request) {
        log.warn("Daily limit exceeded: uri={} error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        HttpStatus.BAD_REQUEST,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(HoldNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleHoldNotFoundException(
            HoldNotFoundException ex, HttpServletRequest request) {
        log.warn("Hold not found: uri={} error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        HttpStatus.BAD_REQUEST,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientFundsException(
            InsufficientFundsException ex, HttpServletRequest request) {
        log.warn("Insufficient funds: uri={} error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: uri={}", request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(
                        "Resource not found: " + request.getRequestURI(),
                        HttpStatus.NOT_FOUND,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "Internal server error",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        request.getRequestURI()
                ));
    }
}
