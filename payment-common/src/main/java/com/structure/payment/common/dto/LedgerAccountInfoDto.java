package com.structure.payment.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Public snapshot of a ledger account.
 * Exposes only the fields other modules legitimately need.
 */
@Data
@Builder
public class LedgerAccountInfoDto {
    private String id;
    private String bankAccountNo;
    private String userId;
    private BigDecimal ledgerBalance;
    private BigDecimal dailyLimit;
    private BigDecimal dailyUsed;
    private String kycTier;
}
