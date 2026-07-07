package com.structure.payment.settlement.repository;

import com.structure.payment.settlement.model.SettlementRecord;
import com.structure.payment.settlement.model.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;


@Repository
public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, String> {
    Optional<SettlementRecord> findByPaymentId(String paymentId);

//    Optional<SettlementRecord> findByProvider_ref(String provider_ref);

    List<SettlementRecord> findByStatus(SettlementStatus status);

    Optional<SettlementRecord> findById(String Id);

    /**
     * Settled records within a date range — used for daily reconciliation.
     */
    @Query("""
        SELECT s FROM SettlementRecord s
        WHERE s.settledAt BETWEEN :from AND :to
          AND s.status = 'SETTLED'
        ORDER BY s.settledAt ASC
        """)
    List<SettlementRecord> findSettledBetween(@Param("from") Instant from,
                                              @Param("to")   Instant to);

    /**
     * Paginated, optionally filtered settlement list.
     */
    @Query("""
        SELECT s FROM SettlementRecord s
        WHERE (:status IS NULL OR s.status = :status)
          AND (:from IS NULL OR s.settledAt >= :from)
          AND (:to   IS NULL OR s.settledAt <= :to)
        """)
    Page<SettlementRecord> findAllWithFilters(@Param("status") SettlementStatus status,
                                              @Param("from")   Instant from,
                                              @Param("to")     Instant to,
                                              Pageable pageable);
}
