package com.structure.payment.payment.controller;

import com.structure.payment.common.dto.BalanceResponse;
import com.structure.payment.payment.client.LedgerApi;
import com.structure.payment.payment.dto.PaymentCommand;
import com.structure.payment.payment.dto.PaymentRequest;
import com.structure.payment.payment.dto.PaymentResponse;
import com.structure.payment.payment.dto.PaymentResult;
import com.structure.payment.payment.service.PaymentSagaOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentSagaOrchestrator sagaOrchestrator;
    private final LedgerApi ledgerApi;

    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        PaymentCommand cmd =
                PaymentCommand.builder()
                        .idempotencyKey(idempotencyKey)
                        .accountId(request.getAccountId())
                        .amount(request.getAmount())
                        .description(request.getDescription())
                        .beneficiaryAcct(request.getBeneficiaryAccount())
                        .build();

        PaymentResult result = sagaOrchestrator.execute(cmd);

        BalanceResponse balance = ledgerApi.getBalance(request.getAccountId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .header("Idempotency-Key", idempotencyKey)
                .body(PaymentResponse.builder()
                        .paymentId(result.getPayment().getId())
                        .status("SUCCESS")
                        .amount(result.getPayment().getAmount())
                        .description(result.getPayment().getDescription())
                        .transactionId(result.getTransaction().getId())
                        .settlementId(result.getSettlement().getProviderRef())
                        .settledAt(result.getSettlement().getSettledAt())
                        .sagaId(result.getSaga().getId())
                        .balance(PaymentResponse.Balance.builder()
                                .ledger(balance.getLedgerBalance())
                                .available(balance.getAvailableBalance())
                                .build())
                        .initiatedAt(result.getPayment().getInitiatedAt())
                        .completedAt(result.getPayment().getCompletedAt())
                        .build());
    }
}
