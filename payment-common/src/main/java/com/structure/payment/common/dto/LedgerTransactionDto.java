package com.structure.payment.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable snapshot of a ledger transaction.
 * Used for cross-service communication so that consumers do not depend on
 * the internal {@code TransactionEntity} JPA class.
 */
@Data
@Builder
public class LedgerTransactionDto {
    private String id;
    private String accountId;
    private String type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String reason;
    private String reference;
    private Instant postedAt;
}
