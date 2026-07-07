package com.structure.payment.settlement.config.Nibss;


import com.structure.payment.common.exception.SettlementException;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssReversalRequest;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssReversalResponse;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssTransferRequest;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssTransferResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;


@Component
@Slf4j
public class MockNibssClient implements NibssClient{

    @Value("${nibss.mock.fail-rate:0.2}")
    private double failRate;
    @Value("${nibss.mock.latency-ms:150}")
    private long latencyMs;

    // Cache to store deterministic failure decisions per session
    // This ensures that if a request fails, all retries will also fail
    private final Map<String, Boolean> failureDecisions = new ConcurrentHashMap<>();

    @Override
    public NibssTransferResponse transfer(NibssTransferRequest request) {
        simulateLatency();

        // Deterministic failure: once decided, always fail/succeed for this session
        boolean shouldFail = failureDecisions.computeIfAbsent(request.getSessionId(), 
            sessionId -> Math.abs(sessionId.hashCode()) % 100 < (failRate * 100));

        if (shouldFail) {
            log.warn("Simulated NIBSS failure for sessionId={} (deterministic)", request.getSessionId());
            throw new SettlementException("SETTLEMENT_FAILED: settlement unavailable");
        }

        String settlementId = "STTL-MOCK-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Transfer settled: sessionId={} settlementId={}",
                request.getSessionId(), settlementId);

        return NibssTransferResponse.builder()
                .settlementId(settlementId)
                .responseCode("00")
                .responseMessage("Approved")
                .settledAt(Instant.now())
                .build();
    }

    @Override
    public NibssReversalResponse reversal(NibssReversalRequest request) {
        simulateLatency();

        return NibssReversalResponse.builder()
                .reversalId("REV-MOCK-"
                        + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .settlementId(request.getSettlementId())
                .responseCode("00")
                .reversedAt(Instant.now())
                .build();
    }

    private void simulateLatency() {
        try {
            long jitter = (long)(Math.random() * 100);
            Thread.sleep(latencyMs + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Clear failure decisions cache periodically to prevent memory leaks.
     * Runs every 6 hours.
     */
    @Scheduled(fixedDelay = 6, timeUnit = TimeUnit.HOURS)
    public void clearFailureCache() {
        int size = failureDecisions.size();
        failureDecisions.clear();
        log.info("Cleared failure decisions cache, removed {} entries", size);
    }
}
