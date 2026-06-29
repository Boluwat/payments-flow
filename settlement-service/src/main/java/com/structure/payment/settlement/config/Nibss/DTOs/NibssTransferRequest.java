package com.structure.payment.settlement.config.Nibss.DTOs;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class NibssTransferRequest {
    private String sessionId;
    private BigDecimal amount;
    private String currency;
    private String beneficiaryAccount;
    private String beneficiaryBank;
    private String beneficiaryName;
    private String senderAccount;
    private String narration;
}
