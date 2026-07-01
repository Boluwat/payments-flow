package com.structure.payment.settlement.config.Nibss.DTOs;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NibssTransferResponse {
    private String  settlementId;
    private String  responseCode;   // "00" = success
    private String  responseMessage;
    private Instant settledAt;
}
