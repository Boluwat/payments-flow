package com.structure.payment.payment.client;

import com.structure.payment.common.dto.SettlementRecordRef;
import com.structure.payment.common.dto.SettlementRequestDto;
import com.structure.payment.common.dto.SettlementResultDto;
import com.structure.payment.common.exception.SettlementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SettlementApiFallbackFactory implements FallbackFactory<SettlementApi> {

    @Override
    public SettlementApi create(Throwable cause) {
        log.error("Settlement service call failed", cause);
        return new SettlementApi() {
            @Override
            public SettlementResultDto settle(SettlementRequestDto request) {
                throw new SettlementException("Settlement service unavailable — cannot process settlement");
            }

            @Override
            public void reverse(String settlementId, String reason) {
                log.warn("Reverse skipped for settlementId={}", settlementId);
            }

            @Override
            public SettlementRecordRef findById(String settlementId) {
                throw new SettlementException("Settlement service unavailable — cannot find settlement");
            }
        };
    }
}
