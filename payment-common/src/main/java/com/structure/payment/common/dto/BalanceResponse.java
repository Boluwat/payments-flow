package com.structure.payment.common.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BalanceResponse {
    private String  accountId;
    private BigDecimal ledgerBalance;
    private BigDecimal    availableBalance;
    private BigDecimal    dailyLimit;
    private BigDecimal   dailyUsed;
    private BigDecimal    dailyRemaining;
    private int     activeHolds;
    private String  kycTier;
}
