package com.structure.payment.common.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Lightweight reference to a settlement record.
 * Used when another service only needs the id and provider reference.
 */
@Data
@Builder
public class SettlementRecordRef {
    private String id;
    private String providerRef;
    private String paymentId;
}
