package com.structure.payment.payment.utils;

import com.structure.payment.common.dto.IdempotencyEntry;

import java.time.Duration;

public interface IdempotencyStore {
    IdempotencyEntry get(String key);
    void setProcessing(String key, Duration ttl);
    void setCompleted(String key, int statusCode, String body, Duration ttl);
    void setFailed(String key, int statusCode, String body, Duration ttl);
    void delete(String key);
}
