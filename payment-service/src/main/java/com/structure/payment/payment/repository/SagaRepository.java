package com.structure.payment.payment.repository;

import com.structure.payment.payment.model.SagaState;
import com.structure.payment.payment.model.SagaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SagaRepository extends JpaRepository<SagaState, String> {

    Optional<SagaState> findByPaymentId(String paymentId);

    List<SagaState> findByStatus(SagaStatus status);

    /**
     * Sagas that need recovery — COMPENSATING but not yet COMPENSATED,
     * or COMPENSATION_FAILED requiring manual intervention.
     */
    @Query("""
        SELECT s FROM SagaState s
        WHERE s.status IN ('COMPENSATING', 'COMPENSATION_FAILED')
        ORDER BY s.startedAt ASC
        """)
    List<SagaState> findSagasNeedingAttention();
}
