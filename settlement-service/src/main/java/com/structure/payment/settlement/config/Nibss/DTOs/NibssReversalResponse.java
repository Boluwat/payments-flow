package com.structure.payment.settlement.config.Nibss.DTOs;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;


@Data
@Builder
public class NibssReversalResponse {
    private String  reversalId;
    private String  settlementId;
    private String  responseCode;
    private Instant reversedAt;
}
