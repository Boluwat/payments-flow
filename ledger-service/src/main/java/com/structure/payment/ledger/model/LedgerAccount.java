package com.structure.payment.ledger.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(
        name = "ledger_accounts",
        indexes =  @Index(name = "idx_ledger_accounts_bank_account_no", columnList = "bank_account_no")
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;

    @Column(name = "owner_name", nullable = false)
    private String userId;

    @Column(name = "bank_account_no", length = 10)
    private String bankAccountNo;

    @Column(name = "ledger_balance", nullable = false)
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Column(name = "daily_limit", nullable = false)
    private BigDecimal dailyLimit = BigDecimal.ZERO;

    /** Running total of outbound transfers today. Resets when date changes. */
    @Column(name = "daily_used", nullable = false)
    @Builder.Default
    private BigDecimal dailyUsed = BigDecimal.ZERO;

    @Column(name = "last_used_date")
    private LocalDate lastUsedDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_tier", nullable = false, length = 10)
    @Builder.Default
    private KycTier kycTier = KycTier.TIER_1;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Hold> holds = new ArrayList<>();

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<TransactionEntity> transactions = new ArrayList<>();

    /** Available to spend: ledger minus all active hold amounts. */
    @Transient
    public BigDecimal getAvailableBalance() {
        BigDecimal totalHeld = holds.stream()
                .filter(h -> h.getStatus() == HoldStatus.ACTIVE)
                .map(Hold::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return ledgerBalance.subtract(totalHeld);
    }
}
