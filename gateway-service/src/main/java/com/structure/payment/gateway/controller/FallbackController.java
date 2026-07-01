package com.structure.payment.gateway.controller;

import com.structure.payment.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Slf4j
@RestController
public class FallbackController {

    @RequestMapping("/fallback/payment")
    public ResponseEntity<ApiResponse<Void>> paymentFallback() {
        log.warn("Payment service fallback triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .message("Payment service is temporarily unavailable. Please try again later.")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @RequestMapping("/fallback/ledger")
    public ResponseEntity<ApiResponse<Void>> ledgerFallback() {
        log.warn("Ledger service fallback triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .message("Ledger service is temporarily unavailable. Please try again later.")
                        .timestamp(LocalDateTime.now())
                        .build());
    }

    @RequestMapping("/fallback/settlement")
    public ResponseEntity<ApiResponse<Void>> settlementFallback() {
        log.warn("Settlement service fallback triggered");
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                        .message("Settlement service is temporarily unavailable. Please try again later.")
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
