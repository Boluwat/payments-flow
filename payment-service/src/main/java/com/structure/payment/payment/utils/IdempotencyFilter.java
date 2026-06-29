package com.structure.payment.payment.utils;

import com.structure.payment.common.dto.IdempotencyEntry;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;


@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private final IdempotencyStore store;

    private static final Duration TTL             = Duration.ofHours(24);
    private static final int      MIN_KEY_LENGTH  = 8;
    private static final int      MAX_KEY_LENGTH  = 255;


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        // Only intercept POST /api/v1/payments
        if (!isPaymentPost(request)) {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getHeader("Idempotency-Key");

        // ── Validate header presence and format ──────────────────────────────
        if (key == null || key.isBlank()) {
            writeError(response, 400,
                    "MISSING_IDEMPOTENCY_KEY",
                    "Idempotency-Key header is required for payment requests");
            return;
        }

        if (key.length() < MIN_KEY_LENGTH || key.length() > MAX_KEY_LENGTH) {
            writeError(response, 400,
                    "INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key must be between 8 and 255 characters");
            return;
        }


        // ── Check store for existing entry
        IdempotencyEntry existing = store.get(key);

        if (existing != null) {
            if (existing.isProcessing()) {
                // In-flight duplicate — block it
                writeError(response, 409,
                        "DUPLICATE_REQUEST",
                        "A request with this Idempotency-Key is already being processed");
                return;
            }

            // Terminal entry (COMPLETED or FAILED) — replay exact response
            log.debug("Replaying cached idempotency response for key={}", key);
            response.setHeader("Idempotency-Key",     key);
            response.setHeader("X-Idempotent-Replayed", "true");
            response.setContentType("application/json");
            response.setStatus(existing.getStatusCode());
            response.getWriter().write(existing.getBody());
            return;
        }

        // ── New key — lock it and proceed ────────────────────────────────────
        store.setProcessing(key, TTL);

        CachedResponseWrapper wrapper = new CachedResponseWrapper(response);

        try {
            chain.doFilter(request, wrapper);
        } catch (Exception ex) {
            // Unexpected filter chain error — delete key so client can retry
            store.delete(key);
            throw ex;
        }


        int     status   = wrapper.getStatus();
        String  body     = wrapper.getCapturedBody();
        boolean terminal = status < 500;   // 5xx = transient, let client retry

        if (terminal) {
            if (status >= 400) {
                store.setFailed(key, status, body, TTL);
            } else {
                store.setCompleted(key, status, body, TTL);
            }
        } else {
            // Transient server error — remove lock so client can retry freely
            store.delete(key);
        }

        response.setHeader("Idempotency-Key", key);
        wrapper.copyBodyToResponse();
    }

    private boolean isPaymentPost(HttpServletRequest req) {
        return "POST".equalsIgnoreCase(req.getMethod())
                && req.getRequestURI().startsWith("/api/v1/payments");
    }

    private void writeError(HttpServletResponse res, int status,
                            String code, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().write(
                String.format("{\"error\":\"%s\",\"message\":\"%s\"}", code, message));
    }
}
