package com.structure.payment.ledger.service;

import com.structure.payment.common.dto.BalanceResponse;
import com.structure.payment.common.dto.LedgerAccountInfoDto;
import com.structure.payment.common.dto.LedgerTransactionDto;
import com.structure.payment.common.exception.AccountNotFoundException;
import com.structure.payment.common.exception.DailyLimitExceededException;
import com.structure.payment.common.exception.HoldNotFoundException;
import com.structure.payment.common.exception.InsufficientFundsException;
import com.structure.payment.ledger.model.Hold;
import com.structure.payment.ledger.model.HoldStatus;
import com.structure.payment.ledger.model.LedgerAccount;
import com.structure.payment.ledger.model.TransactionEntity;
import com.structure.payment.ledger.repository.HoldRepository;
import com.structure.payment.ledger.repository.LedgerAccountRepository;
import com.structure.payment.ledger.repository.TransactionRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class LedgerService {
    private final LedgerAccountRepository accountRepo;
    private final HoldRepository holdRepo;
    private final TransactionRepository txnRepo;

    @Transactional
    public String reserveFunds(String accountId, BigDecimal amount, String reason) {

        // Pessimistic write lock — prevents concurrent race on same account
        LedgerAccount acc = accountRepo.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // availableBalance = ledgerBalance - SUM(active holds)
        BigDecimal totalHeld  = holdRepo.sumActiveHoldsByAccountId(accountId);
        BigDecimal available = acc.getLedgerBalance()
                .subtract(totalHeld != null ? totalHeld : BigDecimal.ZERO);

        if (amount.compareTo(available) > 0) {
            log.warn("Insufficient funds: account={} available={} requested={}",
                    accountId, available, amount);
            throw new InsufficientFundsException("Insufficient funds");
        }

        // ── Guard: daily limit (CBN tier) ─────────────────────────────────
        resetDailyLimitIfNeeded(acc);

        BigDecimal dailyUsed   = acc.getDailyUsed();
        BigDecimal dailyLimit  = acc.getDailyLimit();
        BigDecimal newTotal    = dailyUsed.add(amount);

        if (newTotal.compareTo(dailyLimit) > 0) {
            BigDecimal remaining = dailyLimit.subtract(dailyUsed);

            log.warn("Daily limit exceeded: account={} remaining={} requested={}",
                    accountId, remaining, amount);

            throw new DailyLimitExceededException("Daily limit exceeded");
        }

        // ── Place hold ────────────────────────────────────────────────────
        Hold hold = holdRepo.save(Hold.builder()
                .account(acc)
                .amount(amount)
                .reason(reason)
                .status(HoldStatus.ACTIVE)
                .expiresAt(Instant.now().plusSeconds(300))  // 5-min TTL safety net
                .build());

        accountRepo.save(acc);   // persist any daily-limit reset

        log.info("Hold placed: account={} holdId={} amount={}",
                accountId, hold.getId(), amount);

        return hold.getId();
    }

    @Transactional
    public LedgerTransactionDto captureHold(String accountId, String holdId) {

        LedgerAccount acc = accountRepo.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        Hold hold = holdRepo.findById(holdId)
                .orElseThrow(() -> new HoldNotFoundException(holdId));

        // Debit ledger and accumulate daily usage
        acc.setLedgerBalance(acc.getLedgerBalance().subtract(hold.getAmount()));
        acc.setDailyUsed(acc.getDailyUsed().add(hold.getAmount()));
        hold.setStatus(HoldStatus.CAPTURED);

        holdRepo.save(hold);
        accountRepo.save(acc);

        TransactionEntity txn = txnRepo.save(TransactionEntity.ofDebit(acc, hold, acc.getLedgerBalance(), holdId));

        log.info("Hold captured: account={} holdId={} txnId={} newBalance={}",
                accountId, holdId, txn.getId(), acc.getLedgerBalance());

        return mapToDto(txn);
    }

    @Transactional
    public void releaseHold(String accountId, String holdId) {
        if (holdId == null) return;

        holdRepo.findById(holdId).ifPresent(hold -> {
            if (hold.getStatus() != HoldStatus.ACTIVE) {
                log.debug("releaseHold no-op: holdId={} status={}",
                        holdId, hold.getStatus());
                return;
            }

            LedgerAccount acc = accountRepo.findById(accountId).orElseThrow();
            hold.setStatus(HoldStatus.RELEASED);
            holdRepo.save(hold);
            txnRepo.save(TransactionEntity.ofHoldReleased(acc, hold));

            log.info("Hold released: account={} holdId={}", accountId, holdId);
        });
    }

    @Transactional
    public LedgerTransactionDto postCredit(String accountId, BigDecimal amount,
                                           String reason, String reference) {

        LedgerAccount acc = accountRepo.findByIdWithLock(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        acc.setLedgerBalance(acc.getLedgerBalance().add(amount));
        accountRepo.save(acc);

        TransactionEntity txn = txnRepo.save(
                TransactionEntity.ofCredit(acc, amount, reason, reference));

        log.info("Credit posted: account={} txnId={} amount={} newBalance={}",
                accountId, txn.getId(), amount, acc.getLedgerBalance());

        return mapToDto(txn);
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(String accountId) {
        LedgerAccount acc = accountRepo.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        BigDecimal totalHeld  = holdRepo.sumActiveHoldsByAccountId(accountId);
        BigDecimal available  = acc.getLedgerBalance().subtract(totalHeld);
        BigDecimal remaining  = acc.getDailyLimit().subtract(acc.getDailyUsed());

        return BalanceResponse.builder()
                .accountId(accountId)
                .ledgerBalance(acc.getLedgerBalance())
                .availableBalance(available)
                .dailyLimit(acc.getDailyLimit())
                .dailyUsed(acc.getDailyUsed())
                .dailyRemaining(BigDecimal.ZERO.max(remaining))
                .activeHolds((int) holdRepo.findByAccountIdAndStatus(
                        accountId, HoldStatus.ACTIVE).stream().count())
                .kycTier(acc.getKycTier().name())
                .build();
    }

    @Transactional(readOnly = true)
    public LedgerAccountInfoDto getAccountInfo(String accountId) {
        LedgerAccount acc = accountRepo.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return mapToDto(acc);
    }

    @Transactional(readOnly = true)
    public boolean accountExists(String accountId) {
        return accountRepo.existsById(accountId);
    }

    @Transactional(readOnly = true)
    public List<TransactionEntity> getTransactions(String accountId) {
        accountRepo.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        return txnRepo.findByAccountIdOrderByPostedAtDesc(accountId);
    }

    private void resetDailyLimitIfNeeded(LedgerAccount acc) {
        LocalDate today = LocalDate.now();
        if (!today.equals(acc.getLastUsedDate())) {
            log.debug("Resetting daily usage for account={}", acc.getId());
            acc.setDailyUsed(BigDecimal.ZERO);
            acc.setLastUsedDate(today);
        }
    }

    private LedgerTransactionDto mapToDto(TransactionEntity txn) {
        return LedgerTransactionDto.builder()
                .id(txn.getId())
                .accountId(txn.getAccount().getId())
                .type(txn.getTransactionType().name())
                .amount(txn.getAmount())
                .balanceAfter(txn.getBalanceAfter())
                .reason(txn.getReason())
                .reference(txn.getReference())
                .postedAt(txn.getPostedAt())
                .build();
    }

    private LedgerAccountInfoDto mapToDto(LedgerAccount acc) {
        return LedgerAccountInfoDto.builder()
                .id(acc.getId())
                .bankAccountNo(acc.getBankAccountNo())
                .userId(acc.getUserId())
                .ledgerBalance(acc.getLedgerBalance())
                .dailyLimit(acc.getDailyLimit())
                .dailyUsed(acc.getDailyUsed())
                .kycTier(acc.getKycTier().name())
                .build();
    }
}
