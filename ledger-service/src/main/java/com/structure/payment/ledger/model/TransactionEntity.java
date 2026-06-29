package com.structure.payment.ledger.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Entity
@Table(
        name = "Transactions",
        indexes = {
                @Index(name = "idx_transactions_account_posted", columnList = "account_id, posted_at DESC"),
                @Index(name = "idx_transactions_hold_id",        columnList = "hold_id"),
                @Index(name = "idx_transactions_reference",      columnList = "reference")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private LedgerAccount account;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hold_id")
    private Hold hold;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TransactionType transactionType;

    private BigDecimal amount;

    private BigDecimal balanceAfter;

    private String reason;

    private String reference;

    @CreationTimestamp
    @Column(name = "posted_at", updatable = false)
    private Instant postedAt;

    public static TransactionEntity ofDebit(LedgerAccount account, Hold hold,
                                      BigDecimal balanceAfter, String reference) {
        return TransactionEntity.builder()
                .account(account)
                .hold(hold)
                .transactionType(TransactionType.DEBIT)
                .amount(hold.getAmount())
                .balanceAfter(balanceAfter)
                .reason(hold.getReason())
                .reference(reference)
                .build();
    }

    public static TransactionEntity ofHoldReleased(LedgerAccount account, Hold hold) {
        return TransactionEntity.builder()
                .account(account)
                .hold(hold)
                .transactionType(TransactionType.HOLD_RELEASED)
                .amount(hold.getAmount())
                .balanceAfter(account.getLedgerBalance())
                .reason("Reversal: " + hold.getReason())
                .build();
    }

    public static TransactionEntity ofCredit(LedgerAccount account, BigDecimal amount,
                                       String reason, String reference) {
        return TransactionEntity.builder()
                .account(account)
                .transactionType(TransactionType.CREDIT)
                .amount(amount)
                .balanceAfter(account.getLedgerBalance())
                .reason(reason)
                .reference(reference)
                .build();
    }
}
