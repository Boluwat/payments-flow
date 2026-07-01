package com.structure.payment.ledger.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(
        name = "holds",
        indexes = {
                @Index(name = "idx_holds_account_status", columnList = "account_id, status"),
                @Index(name = "idx_holds_expires_at",     columnList = "expires_at")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Hold {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private LedgerAccount account;

    @Version
    private Long version;

    private BigDecimal amount;

    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private HoldStatus status = HoldStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Background job releases ACTIVE holds past this timestamp.
     * Prevents funds being locked indefinitely if a saga never resolves.
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    @OneToMany(mappedBy = "hold", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TransactionEntity> transactions = new ArrayList<>();

}
