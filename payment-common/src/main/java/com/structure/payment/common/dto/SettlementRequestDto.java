package com.structure.payment.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Everything the settlements service needs to execute a settlement.
 * Deliberately flat — no JPA entities from other services.
 */
@Data
@Builder
public class SettlementRequestDto {
    private String paymentId;
    private BigDecimal amount;
    private String beneficiaryAccountNumber;
    private String beneficiaryName;
    private String description;
    private String senderAccountNo;
}
