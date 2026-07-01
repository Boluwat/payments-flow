package com.structure.payment.payment.dto;

import com.structure.payment.common.dto.LedgerTransactionDto;
import com.structure.payment.common.dto.SettlementResultDto;
import com.structure.payment.payment.model.Payments;
import com.structure.payment.payment.model.SagaState;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResult {
    private Payments payment;
    private SagaState saga;
    private SettlementResultDto settlement;
    private LedgerTransactionDto transaction;
}
