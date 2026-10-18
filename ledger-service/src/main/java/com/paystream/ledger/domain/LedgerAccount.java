package com.paystream.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * An account as the ledger sees it. The balance is a running total of the account's entries and
 * changes only through {@link LedgerTransaction#debit} and {@link LedgerTransaction#credit},
 * so every change has a matching journal entry.
 */
@Entity
@Table(name = "ledger_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerAccount {

    @Id
    private UUID id;

    @Column(nullable = false, length = 20, updatable = false)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private AccountType accountType;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance;

    /** Optimistic lock: an update fails if another transaction changed this row after it was read. */
    @Version
    @Column(nullable = false)
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Whether a debit of this amount is allowed. System accounts may go negative; customer accounts may not.
     */
    public boolean canDebit(BigDecimal amount) {
        return accountType == AccountType.SYSTEM || balance.compareTo(amount) >= 0;
    }

    /** Package-private: only {@link LedgerTransaction} may change a balance, always together with an entry. */
    void debit(BigDecimal amount) {
        requirePositive(amount);
        if (!canDebit(amount)) {
            throw new IllegalStateException("Ledger account " + id + " has insufficient balance for debit of " + amount);
        }
        this.balance = this.balance.subtract(amount);
    }

    /** Package-private: only {@link LedgerTransaction} may change a balance, always together with an entry. */
    void credit(BigDecimal amount) {
        requirePositive(amount);
        this.balance = this.balance.add(amount);
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive but was " + amount);
        }
    }
}