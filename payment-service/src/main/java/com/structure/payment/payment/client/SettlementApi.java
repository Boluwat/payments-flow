package com.structure.payment.payment.client;

import com.structure.payment.common.dto.SettlementRecordRef;
import com.structure.payment.common.dto.SettlementRequestDto;
import com.structure.payment.common.dto.SettlementResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "settlement-service", url = "${settlement-service.url}", fallbackFactory = SettlementApiFallbackFactory.class)
public interface SettlementApi {

    @PostMapping("/api/v1/settlements")
    SettlementResultDto settle(@RequestBody SettlementRequestDto request);

    @PostMapping("/api/v1/settlements/{settlementId}/reverse")
    void reverse(
            @PathVariable("settlementId") String settlementId,
            @RequestParam("reason") String reason);

    @GetMapping("/api/v1/settlements/{settlementId}")
    SettlementRecordRef findById(@PathVariable("settlementId") String settlementId);
}
