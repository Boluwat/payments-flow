package com.structure.payment.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {
    private String  paymentId;
    private String  status;
    private BigDecimal    amount;
    private String  description;
    private String  paymentMethod;
    private String  transactionId;
    private String  settlementId;
    private Instant settledAt;
    private String  sagaId;
    private Balance balance;
    private Instant initiatedAt;
    private Instant completedAt;

    // error fields
    private String  error;
    private String  message;
    private Boolean compensated;

    @Data
    @Builder
    public static class Balance {
        private BigDecimal ledger;
        private BigDecimal available;
    }
}
