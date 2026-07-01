package com.structure.payment.settlement.config;


import com.structure.payment.common.exception.*;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


import java.net.ConnectException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Configuration
@Slf4j
public class PaymentCircuitBreakerConfig {
/**
 * Circuit breaker wrapping the NIBSS settlement service.
 *
 * Transitions:
 *   CLOSED    → OPEN      when ≥50% of last 20 calls fail
 *   OPEN      → HALF_OPEN after 15s
 *   HALF_OPEN → CLOSED    after 2 successful probe calls
 *   HALF_OPEN → OPEN      on probe failure (reset timer)
 *
 * Business exceptions (InsufficientFunds, DailyLimit) are explicitly
 * ignored — they don't indicate a failing downstream, only a bad request.
 */
    @Bean("settlementBreaker")
    public CircuitBreaker settlementCircuitBreaker(CircuitBreakerRegistry registry) {

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(
                        CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(20)
                .failureRateThreshold(60f)
                .minimumNumberOfCalls(6)
                .waitDurationInOpenState(Duration.ofSeconds(15))
                .permittedNumberOfCallsInHalfOpenState(2)
                .recordExceptions(
                        SettlementException.class,
                        ConnectException.class,
                        TimeoutException.class)              // record all by default
                .ignoreExceptions(
                        AccountNotFoundException.class,
                        DailyLimitExceededException.class,
                        HoldNotFoundException.class,
                        InsufficientFundsException.class,
                        DuplicateRequestException.class,
                        PaymentFailedException.class,// business rules — not infra failures
                        ValidationException.class)
                .build();

        CircuitBreaker breaker = registry.circuitBreaker("settlement-service", config);

        // Log every state transition
        breaker.getEventPublisher()
                .onStateTransition(e ->
                        log.info("CircuitBreaker state transition: {} → {}",
                                e.getStateTransition().getFromState(),
                                e.getStateTransition().getToState()));

        return breaker;
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        return CircuitBreakerRegistry.ofDefaults();
    }
}
