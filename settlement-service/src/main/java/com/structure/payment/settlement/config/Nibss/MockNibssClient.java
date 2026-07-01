package com.structure.payment.settlement.config.Nibss;


import com.structure.payment.common.exception.SettlementException;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssReversalRequest;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssReversalResponse;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssTransferRequest;
import com.structure.payment.settlement.config.Nibss.DTOs.NibssTransferResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;


@Component
@Slf4j
public class MockNibssClient implements NibssClient{

    @Value("${nibss.mock.fail-rate:0.2}")
    private double failRate;
    @Value("${nibss.mock.latency-ms:150}")
    private long latencyMs;

    @Override
    public NibssTransferResponse transfer(NibssTransferRequest request) {
        simulateLatency();

        if (Math.random() < failRate) {
            log.warn("Simulated NIBSS failure for sessionId={}", request.getSessionId());
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
}
