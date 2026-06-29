package com.structure.payment.payment.model;

public enum SagaStatus {
    STARTED,
    FUNDS_RESERVED,
    SETTLEMENT_PENDING,
    SETTLEMENT_SUCCEEDED,
    SETTLEMENT_FAILED,
    CAPTURED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    COMPENSATION_FAILED
}
