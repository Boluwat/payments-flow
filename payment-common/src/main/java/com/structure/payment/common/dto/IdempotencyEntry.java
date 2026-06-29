package com.structure.payment.common.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyEntry {
    private String  status;       // PROCESSING | COMPLETED | FAILED
    private Integer statusCode;
    private String  body;
    private Instant expiresAt;

    public boolean isProcessing() { return "PROCESSING".equals(status); }
    public boolean isExpired()    { return Instant.now().isAfter(expiresAt); }
}
