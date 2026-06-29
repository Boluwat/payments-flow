package com.structure.payment.payment.exception;


import com.structure.payment.common.dto.ApiResponse;
import com.structure.payment.common.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class PaymentGlobalExceptionHandler {
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
    public ResponseEntity<ApiResponse<Void>> handleHoldInsufficientFundsException(
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

    @ExceptionHandler(PaymentFailedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentFailedException(
            PaymentFailedException ex, HttpServletRequest request) {
        log.warn("Payment failed: uri={} error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateRequestException(
            DuplicateRequestException ex, HttpServletRequest request) {

        log.warn("Duplicate transaction blocked: key={} existingPayment={}",
                ex.getIdempotencyKey(), ex.getExistingPaymentId());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        HttpStatus.CONFLICT,
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(SettlementException.class)
    public ResponseEntity<ApiResponse<Void>> handleSettlementException(
            SettlementException ex, HttpServletRequest request) {
        log.warn("Settlement error: uri={} error={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error(
                        ex.getMessage(),
                        HttpStatus.BAD_GATEWAY,
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
