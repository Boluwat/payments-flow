package com.structure.payment.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable snapshot of a settlement outcome.
 */
@Data
@Builder
public class SettlementResultDto {
    private String settlementId;
    private String paymentId;
    private String provider;
    private String providerRef;
    private BigDecimal amount;
    private String responseCode;
    private String status;
    private Instant settledAt;
}
