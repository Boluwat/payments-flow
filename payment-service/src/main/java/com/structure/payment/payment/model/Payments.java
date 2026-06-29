package com.structure.payment.payment.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payments_account_id",       columnList = "account_id"),
                @Index(name = "idx_payments_idempotency_key",  columnList = "idempotency_key", unique = true),
                @Index(name = "idx_payments_status",           columnList = "status"),
                @Index(name = "idx_payments_initiated_at",     columnList = "initiated_at DESC")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payments {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    /**
     * Foreign key to the ledger account. Stored as a plain String so that
     * the payment service does not depend on ledger service JPA entities.
     */
    @Column(name = "account_id", nullable = false)
    private String accountId;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private String description;

    private String beneficiaryName;

    @Column(name = "beneficiary_acct", length = 10)
    private String beneficiaryAccountNumber;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "initiated_at", updatable = false)
    private Instant initiatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 255)
    private String idempotencyKey;
}
