package com.structure.payment.settlement.config.Nibss.DTOs;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NibssReversalRequest {
    private String settlementId;
    private String reason;
}
