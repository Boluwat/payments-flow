package com.structure.payment.payment.repository;

import com.structure.payment.payment.model.Idempotency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IdempotencyRepository extends JpaRepository<Idempotency, String> {
    Optional<Idempotency> findByKey(String key);

    /**
     * Rows past their TTL — deleted by the nightly cleanup job.
     */
    @Query("""
        SELECT i FROM Idempotency i
        WHERE i.expiresAt < :now
        """)
    List<Idempotency> findExpiredKeys(@Param("now") Instant now);

    @Modifying
    @Query("""
        DELETE FROM Idempotency i
        WHERE i.expiresAt < :now
        """)
    int deleteExpiredKeys(@Param("now") Instant now);
}
