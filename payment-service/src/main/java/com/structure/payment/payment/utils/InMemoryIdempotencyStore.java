package com.structure.payment.payment.utils;

import com.structure.payment.common.dto.IdempotencyEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class InMemoryIdempotencyStore implements IdempotencyStore {

    private final ConcurrentHashMap<String, IdempotencyEntry> store =
            new ConcurrentHashMap<>();

    @Override
    public IdempotencyEntry get(String key) {
        IdempotencyEntry entry = store.get(key);
        if (entry == null) return null;
        if (entry.isExpired()) {
            store.remove(key);
            return null;
        }
        return entry;
    }

    @Override
    public void setProcessing(String key, Duration ttl) {
        store.put(key, IdempotencyEntry.builder()
                .status("PROCESSING")
                .expiresAt(Instant.now().plus(ttl))
                .build());
    }

    @Override
    public void setCompleted(String key, int statusCode, String body, Duration ttl) {
        store.computeIfPresent(key, (k, e) ->
                IdempotencyEntry.builder()
                        .status("COMPLETED").statusCode(statusCode)
                        .body(body).expiresAt(Instant.now().plus(ttl))
                        .build());
    }

    @Override
    public void setFailed(String key, int statusCode, String body, Duration ttl) {
        store.computeIfPresent(key, (k, e) ->
                IdempotencyEntry.builder()
                        .status("FAILED").statusCode(statusCode)
                        .body(body).expiresAt(Instant.now().plus(ttl))
                        .build());
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }

    /** Evict expired entries every 6 hours to prevent memory growth. */
    @Scheduled(fixedDelay = 6, timeUnit = TimeUnit.HOURS)
    public void evictExpired() {
        int before = store.size();
        store.entrySet().removeIf(e -> e.getValue().isExpired());
        int removed = before - store.size();
        if (removed > 0) log.info("Evicted {} expired idempotency keys", removed);
    }

    public int size() { return store.size(); }
}
