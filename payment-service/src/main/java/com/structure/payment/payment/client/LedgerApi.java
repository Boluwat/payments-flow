package com.structure.payment.payment.client;

import com.structure.payment.common.dto.BalanceResponse;
import com.structure.payment.common.dto.LedgerAccountInfoDto;
import com.structure.payment.common.dto.LedgerTransactionDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "ledger-service", url = "${ledger-service.url}", fallbackFactory = LedgerApiFallbackFactory.class)
public interface LedgerApi {

    @GetMapping("/api/v1/ledger/accounts/{accountId}/exists")
    boolean accountExists(@PathVariable("accountId") String accountId);

    @GetMapping("/api/v1/ledger/accounts/{accountId}")
    LedgerAccountInfoDto getAccountInfo(@PathVariable("accountId") String accountId);

    @GetMapping("/api/v1/ledger/accounts/{accountId}/balance")
    BalanceResponse getBalance(@PathVariable("accountId") String accountId);

    @PostMapping("/api/v1/ledger/accounts/{accountId}/holds")
    String reserveFunds(
            @PathVariable("accountId") String accountId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("reason") String reason);

    @PostMapping("/api/v1/ledger/accounts/{accountId}/holds/{holdId}/capture")
    LedgerTransactionDto captureHold(
            @PathVariable("accountId") String accountId,
            @PathVariable("holdId") String holdId);

    @PostMapping("/api/v1/ledger/accounts/{accountId}/holds/{holdId}/release")
    void releaseHold(
            @PathVariable("accountId") String accountId,
            @PathVariable("holdId") String holdId);

    @PostMapping("/api/v1/ledger/accounts/{accountId}/credits")
    LedgerTransactionDto postCredit(
            @PathVariable("accountId") String accountId,
            @RequestParam("amount") BigDecimal amount,
            @RequestParam("reason") String reason,
            @RequestParam("reference") String reference);
}
