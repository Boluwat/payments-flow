package com.structure.payment.settlement.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

@Component
@Slf4j
public class RetryWithJitter {
    private static final int  MAX_ATTEMPTS = 3;
    private static final long BASE_MS      = 300L;
    private static final long CAP_MS       = 3_000L;

    private final Random random = new Random();

    /**
     * Retry with full-jitter backoff.
     * sleep = random(0, min(cap, base * 2^attempt))
     * Retries all exceptions by default.
     */
    public <T> T execute(Callable<T> fn) throws Exception {
        return execute(fn, ex -> true);
    }

    /**
     * Retry with custom predicate controlling which exceptions are retried.
     * Business rule exceptions (InsufficientFunds, DailyLimit) should NOT
     * be retried — they will always fail.
     */
    public <T> T execute(Callable<T> fn,
                         Predicate<Exception> retryIf) throws Exception {
        Exception last = null;

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return fn.call();
            } catch (Exception ex) {
                last = ex;

                boolean isLastAttempt = (attempt == MAX_ATTEMPTS - 1);
                if (isLastAttempt || !retryIf.test(ex)) {
                    throw ex;
                }

                // Full jitter: sleep up to min(cap, base * 2^attempt)
                long ceiling = Math.min(CAP_MS, BASE_MS * (1L << attempt));
                long delayMs = (long) (random.nextDouble() * ceiling);

                log.warn("Attempt {} failed ({}), retrying in {}ms",
                        attempt + 1, ex.getMessage(), delayMs);

                Thread.sleep(delayMs);
            }
        }
        throw last;
    }
}
