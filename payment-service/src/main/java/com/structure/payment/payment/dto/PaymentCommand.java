package com.structure.payment.payment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentCommand {
    private String idempotencyKey;
    private String accountId;
    private BigDecimal amount;
    private String  description;
//    private PaymentMethod paymentMethod;
    private String beneficiaryAcct;
//    private String                   beneficiaryBank;
    private String beneficiaryName;
}
