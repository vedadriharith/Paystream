package com.paystream.payment.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

/**
 * A payment request from a payer to a payee. State changes only through
 * {@link #markCompleted()} and {@link #markFailed(String)}, which enforce valid transitions.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(nullable = false, length = 64, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false, length = 20, updatable = false)
    private String payerAccount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 64, updatable = false)
    private String requestHash;

    @Column(nullable = false, length = 20, updatable = false)
    private String payeeAccount;

    @Column(nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3, updatable = false)
    private String currency;

    @Column(length = 140, updatable = false)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 200)
    private String failureReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** True until the entity is first persisted or loaded; lets Spring Data skip a SELECT before INSERT. */
    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    /**
     * Creates a new payment in INITIATED state with an application-generated id.
     */
    public static Payment initiate(String idempotencyKey,
                                   String requestHash,
                                   String payerAccount,
                                   String payeeAccount,
                                   BigDecimal amount,
                                   String currency,
                                   String reference) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.idempotencyKey = idempotencyKey;
        payment.requestHash = requestHash;
        payment.payerAccount = payerAccount;
        payment.payeeAccount = payeeAccount;
        payment.amount = amount;
        payment.currency = currency;
        payment.reference = reference;
        payment.status = PaymentStatus.INITIATED;
        return payment;
    }

    public void markCompleted() {
        requireStatus(PaymentStatus.INITIATED);
        this.status = PaymentStatus.COMPLETED;
    }

    public void markFailed(String reason) {
        requireStatus(PaymentStatus.INITIATED);
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    private void requireStatus(PaymentStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Payment " + id + " must be " + expected + " but is " + status);
        }
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