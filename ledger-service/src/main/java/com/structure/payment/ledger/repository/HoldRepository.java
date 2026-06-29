package com.structure.payment.ledger.repository;

import com.structure.payment.ledger.model.Hold;
import com.structure.payment.ledger.model.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface HoldRepository extends JpaRepository<Hold, String> {
    List<Hold> findByAccountIdAndStatus(String accountId, HoldStatus status);

    /**
     * Finds holds that are still ACTIVE but have passed their expiry.
     * Used by the background hold-expiry job.
     */
    @Query("""
        SELECT h FROM Hold h
        WHERE h.status = 'ACTIVE'
          AND h.expiresAt < :now
        """)
    List<Hold> findExpiredActiveHolds(@Param("now") Instant now);

    /**
     * Sum of all active hold amounts for an account.
     * Used to compute availableBalance without loading the hold list.
     */
    @Query("""
        SELECT COALESCE(SUM(h.amount), 0)
        FROM Hold h
        WHERE h.account.id = :accountId
          AND h.status = 'ACTIVE'
        """)
    BigDecimal sumActiveHoldsByAccountId(@Param("accountId") String accountId);
}
