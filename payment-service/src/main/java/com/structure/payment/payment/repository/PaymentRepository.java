package com.structure.payment.payment.repository;

import com.structure.payment.payment.model.PaymentStatus;
import com.structure.payment.payment.model.Payments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payments, String> {
    Optional<Payments> findByIdempotencyKey(String idempotencyKey);

    List<Payments> findByAccountIdOrderByInitiatedAtDesc(String accountId);

    List<Payments> findByStatus(PaymentStatus status);

    /**
     * Payments still PENDING beyond a threshold — candidates for saga recovery.
     */
    @Query("""
        SELECT p FROM Payments p
        WHERE p.status = 'PENDING'
          AND p.initiatedAt < :before
        """)
    List<Payments> findStalePendingPayments(@Param("before") Instant before);
}
