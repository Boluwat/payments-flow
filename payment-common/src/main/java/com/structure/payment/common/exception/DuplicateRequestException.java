package com.structure.payment.common.exception;

import lombok.Getter;

@Getter
public class DuplicateRequestException extends RuntimeException {
    private final String idempotencyKey;
    private final String existingPaymentId;
    public DuplicateRequestException(String idempotencyKey, String existingPaymentId) {
        super(String.format(
                "A successful transaction already exists for idempotency key '%s' " +
                        "with payment id '%s'",
                idempotencyKey, existingPaymentId));;
        this.idempotencyKey = idempotencyKey;
        this.existingPaymentId = existingPaymentId;
    }
}
