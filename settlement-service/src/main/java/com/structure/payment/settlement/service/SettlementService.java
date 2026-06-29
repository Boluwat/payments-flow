package com.structure.payment.settlement.service;

import com.structure.payment.common.dto.SettlementRecordRef;
import com.structure.payment.common.dto.SettlementRequestDto;
import com.structure.payment.common.dto.SettlementResultDto;
import com.structure.payment.common.exception.SettlementException;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssReversalRequest;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssReversalResponse;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssTransferRequest;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssTransferResponse;
import com.structure.payment.settlement.config.Nibss.NibssClient;
import com.structure.payment.settlement.config.RetryWithJitter;
import com.structure.payment.settlement.model.SettlementRecord;
import com.structure.payment.settlement.model.SettlementStatus;
import com.structure.payment.settlement.repository.SettlementRecordRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@Slf4j
public class SettlementService {

    private final NibssClient nibssClient;
    private final SettlementRecordRepository settlementRepo;
    private final RetryWithJitter retry;
    private final CircuitBreaker circuitBreaker;

    public SettlementService(NibssClient nibssClient, SettlementRecordRepository settlementRepo, RetryWithJitter retry, CircuitBreaker circuitBreaker) {
        this.nibssClient = nibssClient;
        this.settlementRepo = settlementRepo;
        this.retry = retry;
        this.circuitBreaker = circuitBreaker;
    }

    @Transactional
    public SettlementResultDto settle(SettlementRequestDto request) {
        log.info("Attempting settlement: paymentId={} amount={}",
                request.getPaymentId(), request.getAmount());

        NibssTransferRequest nibssRequest = NibssTransferRequest.builder()
                .sessionId(request.getPaymentId())
                .amount(request.getAmount())
                .beneficiaryAccount(request.getBeneficiaryAccountNumber())
                .beneficiaryName(request.getBeneficiaryName())
                .senderAccount(request.getSenderAccountNo())
                .narration(request.getDescription())
                .build();

        log.info("Calling NibssClient implementation: {}",
                nibssClient.getClass().getSimpleName());

        NibssTransferResponse response;

        try {
            // Circuit breaker wraps the ENTIRE retry block so that retries
            // count as a single call in the CB statistics.
            response = circuitBreaker.executeCallable(
                    () -> retry.execute(
                            () -> nibssClient.transfer(nibssRequest),
                            // Only retry transient errors — not business rejections
                            ex -> ex instanceof SettlementException
                    )
            );
        } catch (CallNotPermittedException ex) {
            log.error("Circuit breaker OPEN — fast failing paymentId={}",
                    request.getPaymentId());
            throw new SettlementException("CIRCUIT_OPEN: Settlement service is temporarily unavailable — try again shortly");
        } catch (SettlementException ex) {
            log.error("Settlement failed after retries: paymentId={}", request.getPaymentId(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected settlement error: paymentId={}", request.getPaymentId(), ex);
            throw new SettlementException(ex.getMessage());
        }

        // Persist settlement record
        SettlementRecord record = settlementRepo.save(SettlementRecord.builder()
                        .paymentId(request.getPaymentId())
                        .provider("Mock")
                        .provider_ref(response.getSettlementId())
                        .amount(request.getAmount())
                        .responseCode(response.getResponseCode())
                        .status(SettlementStatus.SETTLED)
                        .settledAt(response.getSettledAt())
                        .build());

        log.info("Settled: paymentId={} settlementId={} providerRef={}",
                request.getPaymentId(), record.getId(), record.getProvider_ref());

        return mapToDto(record);
    }

    public void reverse(SettlementRecordRef recordRef, String reason) {
        if (recordRef == null) return;

        SettlementRecord record = settlementRepo.findById(recordRef.getId())
                .orElseThrow(() -> new SettlementException("Settlement not found"));

        try {
            NibssReversalResponse response = nibssClient.reversal(
                    NibssReversalRequest.builder()
                            .settlementId(record.getProvider_ref())
                            .reason(reason)
                            .build());

            record.setStatus(SettlementStatus.REVERSED);
            record.setReversedAt(Instant.now());
            record.setReversalRef(response.getReversalId());
            settlementRepo.save(record);

            log.info("Reversed: settlementId={} reversalId={}",
                    record.getProvider_ref(), record.getReversalRef());

        } catch (Exception ex) {
            log.error("Reversal failed for settlementId={} — requires manual review",
                    record.getProvider_ref(), ex);
            throw new SettlementException("REVERSAL_FAILED: " + ex.getMessage());
        }
    }

    public SettlementRecordRef findById(String settlementId) {
        SettlementRecord record = settlementRepo.findById(settlementId)
                .orElseThrow(() -> new SettlementException("Settlement not found"));
        return SettlementRecordRef.builder()
                .id(record.getId())
                .providerRef(record.getProvider_ref())
                .paymentId(record.getPaymentId())
                .build();
    }

    private SettlementResultDto mapToDto(SettlementRecord record) {
        return SettlementResultDto.builder()
                .settlementId(record.getId())
                .paymentId(record.getPaymentId())
                .provider(record.getProvider())
                .providerRef(record.getProvider_ref())
                .amount(record.getAmount())
                .responseCode(record.getResponseCode())
                .status(record.getStatus().name())
                .settledAt(record.getSettledAt())
                .build();
    }
}
