package com.structure.payment.payment.service;

import com.structure.payment.common.dto.LedgerAccountInfoDto;
import com.structure.payment.common.dto.LedgerTransactionDto;
import com.structure.payment.common.exception.AccountNotFoundException;
import com.structure.payment.common.exception.DuplicateRequestException;
import com.structure.payment.common.exception.PaymentFailedException;
import com.structure.payment.common.exception.SettlementException;
import com.structure.payment.payment.client.LedgerApi;
import com.structure.payment.payment.client.SettlementApi;
import com.structure.payment.payment.dto.PaymentCommand;
import com.structure.payment.payment.dto.PaymentResult;
import com.structure.payment.payment.model.PaymentStatus;
import com.structure.payment.payment.model.Payments;
import com.structure.payment.payment.model.SagaState;
import com.structure.payment.payment.model.SagaStatus;
import com.structure.payment.payment.repository.PaymentRepository;
import com.structure.payment.payment.repository.SagaRepository;
import com.structure.payment.common.dto.SettlementRequestDto;
import com.structure.payment.common.dto.SettlementResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSagaOrchestrator {
    private final LedgerApi ledgerApi;
    private final SettlementApi settlementApi;
    private final PaymentRepository paymentRepo;
    private final SagaRepository sagaRepo;

    @Autowired
    @Lazy
    private PaymentSagaOrchestrator self;

    /**
     * Orchestrates the payment saga. Intentionally NON-TRANSACTIONAL:
     * the saga spans multiple remote services, so holding a DB connection
     * open across Feign calls starves the pool and triggers leak detection.
     * Each local DB mutation is handled in a short {@literal @}Transactional helper.
     */
    public PaymentResult execute(PaymentCommand cmd) {

        // ── Validate account exists (remote call, no DB needed)
        if (!ledgerApi.accountExists(cmd.getAccountId())) {
            throw new AccountNotFoundException("Account not found: " + cmd.getAccountId());
        }

        // ── Create Payment + Saga (short transaction; handles idempotency inside)
        SagaInit init = self.initialize(cmd);
        Payments payment = init.payment();
        SagaState saga = init.saga();

        log.info("Saga started: sagaId={} payment={} account={} amount={}",
                saga.getId(), payment.getId(), cmd.getAccountId(), cmd.getAmount());

        // ── Step 1: Reserve funds ────────────────────────────────────────────
        String holdId;
        try {
            holdId = ledgerApi.reserveFunds(
                    cmd.getAccountId(), cmd.getAmount(), cmd.getDescription());
            if (holdId == null || holdId.isBlank()) {
                throw new PaymentFailedException("RESERVE_FUNDS returned empty holdId");
            }
            saga.setHoldId(holdId);
            self.transition(saga, SagaStatus.FUNDS_RESERVED,
                    "RESERVE_FUNDS", holdId);
        } catch (PaymentFailedException ex) {
            // No hold placed — nothing to compensate
            self.failPayment(payment, saga, "RESERVE_FUNDS", ex);
            throw ex;
        }

        // ── Step 2: Settle ───────────────────────────────────────────────────
        LedgerAccountInfoDto accountInfo = ledgerApi.getAccountInfo(cmd.getAccountId());

        SettlementResultDto settlementResult;
        try {
            self.transition(saga, SagaStatus.SETTLEMENT_PENDING,
                    "SETTLEMENT", "Calling NIBSS");

            SettlementRequestDto settlementRequest = SettlementRequestDto.builder()
                    .paymentId(payment.getId())
                    .amount(payment.getAmount())
                    .beneficiaryAccountNumber(payment.getBeneficiaryAccountNumber())
                    .beneficiaryName(payment.getBeneficiaryName())
                    .description(payment.getDescription())
                    .senderAccountNo(accountInfo.getBankAccountNo())
                    .build();

            settlementResult = settlementApi.settle(settlementRequest);
            saga.setSettlementId(settlementResult.getSettlementId());
            self.transition(saga, SagaStatus.SETTLEMENT_SUCCEEDED,
                    "SETTLEMENT", settlementResult.getProviderRef());

        } catch (Exception ex) {
            log.warn("Settlement failed — compensating: sagaId={}", saga.getId());

            log.error(
                    "Settlement failed: sagaId={}",
                    saga.getId(),
                    ex
            );

            self.transition(saga, SagaStatus.SETTLEMENT_FAILED,
                    "SETTLEMENT", ex.getMessage());

            // Compensate: release hold (settlement never completed)
            self.compensate(saga, cmd.getAccountId(), cmd.getAmount(),
                    cmd.getDescription(), false);
            self.failPayment(payment, saga, "SETTLEMENT", ex);

            if (ex instanceof SettlementException se) {
                throw se;
            }
            throw new SettlementException("SETTLEMENT_FAILED: " + ex.getMessage());
        }

        // ── Step 3: Capture hold ─────────────────────────────────────────────
        LedgerTransactionDto txn;
        try {
            txn = ledgerApi.captureHold(cmd.getAccountId(), holdId);
            self.transition(saga, SagaStatus.COMPLETED,
                    "CAPTURE_HOLD", txn.getId());
            self.markSuccess(payment, saga);

            log.info("Saga completed: sagaId={} payment={} txn={} settlement={}",
                    saga.getId(), payment.getId(), txn.getId(),
                    settlementResult.getProviderRef());

            return PaymentResult.builder()
                    .payment(payment)
                    .saga(saga)
                    .settlement(settlementResult)
                    .transaction(txn)
                    .build();

        } catch (Exception ex) {
            // Extremely rare: capture failed AFTER settlement succeeded
            // Must reverse the settlement and post a credit
            log.error("Capture failed post-settlement — reversing: sagaId={}",
                    saga.getId(), ex);
            self.transition(saga, SagaStatus.SETTLEMENT_FAILED,
                    "CAPTURE_HOLD", ex.getMessage());

            self.compensate(saga, cmd.getAccountId(), cmd.getAmount(),
                    cmd.getDescription(), true);
            self.failPayment(payment, saga, "CAPTURE_HOLD", ex);

            throw wrapIfNeeded(ex, saga);
        }
    }

    @Transactional
    public SagaInit initialize(PaymentCommand cmd) {
        // Idempotency check inside the tx narrows the race window
        Optional<Payments> existing = paymentRepo.findByIdempotencyKey(cmd.getIdempotencyKey());
        if (existing.isPresent()) {
            if (existing.get().getStatus() == PaymentStatus.SUCCESS) {
                throw new DuplicateRequestException(
                        cmd.getIdempotencyKey(), existing.get().getId());
            }
            // If status is FAILED / PENDING, we can't INSERT again because the
            // DB has a unique constraint on idempotency_key. Treat as duplicate.
            throw new DuplicateRequestException(
                    cmd.getIdempotencyKey(), existing.get().getId());
        }

        try {
            Payments payment = paymentRepo.save(Payments.builder()
                    .accountId(cmd.getAccountId())
                    .idempotencyKey(cmd.getIdempotencyKey())
                    .status(PaymentStatus.PENDING)
                    .amount(cmd.getAmount())
                    .description(cmd.getDescription())
                    .beneficiaryAccountNumber(cmd.getBeneficiaryAcct())
                    .beneficiaryName(cmd.getBeneficiaryName())
                    .build());

            SagaState saga = sagaRepo.save(SagaState.builder()
                    .payment(payment)
                    .status(SagaStatus.STARTED)
                    .build());

            return new SagaInit(payment, saga);
        } catch (DataIntegrityViolationException ex) {
            // Another thread/request won the race between find() and save()
            Payments conflict = paymentRepo.findByIdempotencyKey(cmd.getIdempotencyKey())
                    .orElseThrow(() -> new IllegalStateException(
                            "Idempotency key conflict but record not found: "
                                    + cmd.getIdempotencyKey()));
            throw new DuplicateRequestException(cmd.getIdempotencyKey(), conflict.getId());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void compensate(SagaState saga, String accountId,
                           BigDecimal amount, String description,
                           boolean reversalNeeded) {
        // Reload from DB to ensure we read the persisted holdId / settlementId
        SagaState fresh = sagaRepo.findById(saga.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Saga not found during compensation: " + saga.getId()));

        transition(fresh, SagaStatus.COMPENSATING, "COMPENSATE", null);

        try {
            // Always release the hold — idempotent if already released
            if (fresh.getHoldId() != null && !fresh.getHoldId().isBlank()) {
                ledgerApi.releaseHold(accountId, fresh.getHoldId());
            } else {
                log.warn("No holdId present — skipping releaseHold: sagaId={}", fresh.getId());
            }

            // If settlement succeeded before capture failed, also reverse settlement
            if (reversalNeeded && fresh.getSettlementId() != null
                    && !fresh.getSettlementId().isBlank()) {
                var settlementRef = settlementApi.findById(fresh.getSettlementId());
                settlementApi.reverse(settlementRef.getId(),
                        "Compensation: " + description);
                ledgerApi.postCredit(accountId, amount,
                        "Reversal: " + description, fresh.getPayment().getId());
            }

            transition(fresh, SagaStatus.COMPENSATED, "COMPENSATE", "success");
            log.info("Compensation complete: sagaId={}", fresh.getId());

        } catch (Exception compEx) {
            // Compensation itself failed — critical, needs dead-letter / ops alert
            transition(fresh, SagaStatus.COMPENSATION_FAILED,
                    "COMPENSATE", compEx.getMessage());
            log.error("Compensation failed — manual review required: sagaId={}",
                    fresh.getId(), compEx);
        }
    }

    @Transactional
    public void transition(SagaState saga, SagaStatus status,
                           String stepName, Object data) {
        saga.transition(status, stepName, data);
        sagaRepo.save(saga);
    }

    @Transactional
    public void failPayment(Payments payment, SagaState saga,
                            String step, Exception ex) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setCompletedAt(Instant.now());
        paymentRepo.save(payment);
        saga.fail(step, ex.getMessage());
        sagaRepo.save(saga);
    }

    @Transactional
    public void markSuccess(Payments payment, SagaState saga) {
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setCompletedAt(Instant.now());
        paymentRepo.save(payment);
    }

    public Optional<SagaState> findSaga(String paymentId) {
        return sagaRepo.findByPaymentId(paymentId);
    }

    private RuntimeException wrapIfNeeded(Exception ex, SagaState saga) {
        if (ex instanceof PaymentFailedException || ex instanceof SettlementException) {
            return (RuntimeException) ex;
        }
        return new PaymentFailedException("PAYMENT_FAILED: " + ex.getMessage()) {};
    }

    private record SagaInit(Payments payment, SagaState saga) {
    }
}
