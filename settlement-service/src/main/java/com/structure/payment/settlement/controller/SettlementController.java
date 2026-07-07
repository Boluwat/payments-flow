package com.structure.payment.settlement.controller;

import com.structure.payment.common.dto.SettlementRecordRef;
import com.structure.payment.common.dto.SettlementRequestDto;
import com.structure.payment.common.dto.SettlementResultDto;
import com.structure.payment.settlement.model.SettlementStatus;
import com.structure.payment.settlement.service.SettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    public ResponseEntity<SettlementResultDto> settle(@RequestBody SettlementRequestDto request) {
        return ResponseEntity.ok(settlementService.settle(request));
    }

    @PostMapping("/{settlementId}/reverse")
    public ResponseEntity<Void> reverse(
            @PathVariable String settlementId,
            @RequestParam String reason) {
        SettlementRecordRef ref = settlementService.findById(settlementId);
        settlementService.reverse(ref, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{settlementId}")
    public ResponseEntity<SettlementRecordRef> findById(@PathVariable String settlementId) {
        return ResponseEntity.ok(settlementService.findById(settlementId));
    }

    @GetMapping("/settlements")
    public ResponseEntity<Page<SettlementResultDto>> findAll(
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("settledAt").descending());
        return ResponseEntity.ok(settlementService.findAllSettlements(status, from, to, pageable));
    }
}
