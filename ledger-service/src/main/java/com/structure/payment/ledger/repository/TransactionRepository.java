package com.structure.payment.ledger.repository;

import com.structure.payment.ledger.model.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, String> {
    /**
     * Ordered ledger history for a given account — newest first.
     */
    @Query("""
        SELECT t FROM TransactionEntity t
        WHERE t.account.id = :accountId
        ORDER BY t.postedAt DESC
        """)
    List<TransactionEntity> findByAccountIdOrderByPostedAtDesc(@Param("accountId") String accountId);

    List<TransactionEntity> findByReference(String reference);

//    Optional<TransactionEntity> findByHoldIdAndType(String holdId, TransactionType type);
}
