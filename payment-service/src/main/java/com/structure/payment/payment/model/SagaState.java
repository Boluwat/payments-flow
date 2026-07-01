package com.structure.payment.payment.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "saga_state")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaState {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payments payment;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    @Column(name = "hold_id")
    private String holdId;

    @Column(name = "settlement_id")
    private String settlementId;

    /**
     * JSONB column (PostgreSQL)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "steps", columnDefinition = "jsonb")
    @Builder.Default
    private List<SagaStep> steps = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private Instant startedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public void transition(SagaStatus newStatus, String stepName, Object data) {
        this.status = newStatus;
        this.steps.add(SagaStep.builder()
                .name(stepName)
                .status(newStatus.name())
                .data(data)
                .at(Instant.now())
                .build());
    }

    public void fail(String stepName, String errorMessage) {
        this.status = SagaStatus.COMPENSATION_FAILED;
        this.steps.add(SagaStep.builder()
                .name(stepName)
                .status("FAILED")
                .data(errorMessage)
                .at(Instant.now())
                .build());
    }
}
