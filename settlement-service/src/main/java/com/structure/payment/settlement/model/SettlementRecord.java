package com.structure.payment.settlement.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "Settlement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    /**
     * Foreign key to the payment. Stored as a plain String so that
     * the settlement service does not depend on payment-service JPA entities.
     */
    @Column(name = "payment_id", nullable = false, unique = true)
    private String paymentId;

    private String provider;

    private String provider_ref;

    private BigDecimal amount;

    private String responseCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.SETTLED;

    @CreationTimestamp
    @Column(name = "settled_at", updatable = false)
    private Instant settledAt;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    private String reversalRef;
}
