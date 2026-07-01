package com.structure.payment.ledger.controller;

import com.structure.payment.common.dto.BalanceResponse;
import com.structure.payment.common.dto.LedgerAccountInfoDto;
import com.structure.payment.common.dto.LedgerTransactionDto;
import com.structure.payment.ledger.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
public class LedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/accounts/{accountId}/exists")
    public ResponseEntity<Boolean> accountExists(@PathVariable String accountId) {
        return ResponseEntity.ok(ledgerService.accountExists(accountId));
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<LedgerAccountInfoDto> getAccountInfo(@PathVariable String accountId) {
        return ResponseEntity.ok(ledgerService.getAccountInfo(accountId));
    }

    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BalanceResponse> getBalance(@PathVariable String accountId) {
        return ResponseEntity.ok(ledgerService.getBalance(accountId));
    }

    @PostMapping("/accounts/{accountId}/holds")
    public ResponseEntity<String> reserveFunds(
            @PathVariable String accountId,
            @RequestParam BigDecimal amount,
            @RequestParam String reason) {
        return ResponseEntity.ok(ledgerService.reserveFunds(accountId, amount, reason));
    }

    @PostMapping("/accounts/{accountId}/holds/{holdId}/capture")
    public ResponseEntity<LedgerTransactionDto> captureHold(
            @PathVariable String accountId,
            @PathVariable String holdId) {
        return ResponseEntity.ok(ledgerService.captureHold(accountId, holdId));
    }

    @PostMapping("/accounts/{accountId}/holds/{holdId}/release")
    public ResponseEntity<Void> releaseHold(
            @PathVariable String accountId,
            @PathVariable String holdId) {
        ledgerService.releaseHold(accountId, holdId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/accounts/{accountId}/credits")
    public ResponseEntity<LedgerTransactionDto> postCredit(
            @PathVariable String accountId,
            @RequestParam BigDecimal amount,
            @RequestParam String reason,
            @RequestParam String reference) {
        return ResponseEntity.ok(ledgerService.postCredit(accountId, amount, reason, reference));
    }
}
