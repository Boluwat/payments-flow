package com.structure.payment.payment.client;

import com.structure.payment.common.dto.BalanceResponse;
import com.structure.payment.common.dto.LedgerAccountInfoDto;
import com.structure.payment.common.dto.LedgerTransactionDto;
import com.structure.payment.common.exception.AccountNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
public class LedgerApiFallbackFactory implements FallbackFactory<LedgerApi> {

    @Override
    public LedgerApi create(Throwable cause) {
        log.error("Ledger service call failed", cause);
        return new LedgerApi() {
            @Override
            public boolean accountExists(String accountId) {
                throw new AccountNotFoundException("Ledger service unavailable — cannot verify account");
            }

            @Override
            public LedgerAccountInfoDto getAccountInfo(String accountId) {
                throw new AccountNotFoundException("Ledger service unavailable — cannot retrieve account info");
            }

            @Override
            public BalanceResponse getBalance(String accountId) {
                throw new AccountNotFoundException("Ledger service unavailable — cannot retrieve balance");
            }

            @Override
            public String reserveFunds(String accountId, BigDecimal amount, String reason) {
                throw new AccountNotFoundException("Ledger service unavailable — cannot reserve funds");
            }

            @Override
            public LedgerTransactionDto captureHold(String accountId, String holdId) {
                throw new AccountNotFoundException("Ledger service unavailable — cannot capture hold");
            }

            @Override
            public void releaseHold(String accountId, String holdId) {
                log.warn("ReleaseHold skipped for account={}, holdId={}", accountId, holdId);
            }

            @Override
            public LedgerTransactionDto postCredit(String accountId, BigDecimal amount, String reason, String reference) {
                throw new AccountNotFoundException("Ledger service unavailable — cannot post credit");
            }
        };
    }
}
