package com.paystream.ledger.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

/**
 * One business event that moves money, made of balanced entries (total debits = total credits).
 * Saving the transaction also saves its entries.
 */
@Entity
@Table(name = "ledger_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LedgerTransaction implements Persistable<UUID> {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private LedgerTransactionType type;

    @Column(updatable = false)
    private UUID paymentId;

    @Column(length = 200, updatable = false)
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.PERSIST)
    @Getter(AccessLevel.NONE)
    private List<LedgerEntry> entries = new ArrayList<>();

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static LedgerTransaction forPayment(UUID paymentId, String description) {
        LedgerTransaction transaction = new LedgerTransaction();
        transaction.id = UUID.randomUUID();
        transaction.type = LedgerTransactionType.PAYMENT;
        transaction.paymentId = Objects.requireNonNull(paymentId, "paymentId");
        transaction.description = description;
        return transaction;
    }

    /** Decreases the account's balance and records the matching DEBIT entry. */
    public void debit(LedgerAccount account, BigDecimal amount) {
        account.debit(amount);
        entries.add(LedgerEntry.create(this, account, EntryDirection.DEBIT, amount));
    }

    /** Increases the account's balance and records the matching CREDIT entry. */
    public void credit(LedgerAccount account, BigDecimal amount) {
        account.credit(amount);
        entries.add(LedgerEntry.create(this, account, EntryDirection.CREDIT, amount));
    }

    public List<LedgerEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Runs right before INSERT, so an unbalanced transaction can never be stored.
     * Uses compareTo, not equals: BigDecimal equals also compares scale (100.00 vs 100.0000).
     */
    @PrePersist
    void verifyBalanced() {
        if (entries.size() < 2) {
            throw new IllegalStateException("Ledger transaction " + id + " needs at least 2 entries");
        }
        long currencies = entries.stream().map(LedgerEntry::getCurrency).distinct().count();
        if (currencies != 1) {
            throw new IllegalStateException("Ledger transaction " + id + " mixes currencies");
        }
        BigDecimal debits = total(EntryDirection.DEBIT);
        BigDecimal credits = total(EntryDirection.CREDIT);
        if (debits.compareTo(credits) != 0) {
            throw new IllegalStateException(
                    "Ledger transaction " + id + " is unbalanced: debits " + debits + " != credits " + credits);
        }
    }

    private BigDecimal total(EntryDirection direction) {
        return entries.stream()
                .filter(entry -> entry.getDirection() == direction)
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.newEntity = false;
    }
}