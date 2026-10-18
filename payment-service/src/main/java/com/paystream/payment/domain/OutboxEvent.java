package com.paystream.payment.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.domain.Persistable;

/**
 * An event waiting to be relayed to Kafka. Written in the same transaction as the business change,
 * so the event exists if and only if the change was committed.
 */
@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent implements Persistable<UUID> {

    private static final int MAX_ERROR_LENGTH = 500;

    @Id
    private UUID id;

    @Column(nullable = false, length = 60, updatable = false)
    private String aggregateType;

    @Column(nullable = false, updatable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 80, updatable = false)
    private String eventType;

    /** Serialized event envelope. Stored as JSONB; the cast lets a Java String be written to it. */
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String payload;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(length = MAX_ERROR_LENGTH)
    private String lastError;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean newEntity = true;

    public static OutboxEvent pending(UUID eventId,
                                      String aggregateType,
                                      UUID aggregateId,
                                      String eventType,
                                      String payload) {
        OutboxEvent event = new OutboxEvent();
        event.id = eventId;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.payload = payload;
        return event;
    }

    public void markPublished(Instant when) {
        this.publishedAt = when;
        this.attempts++;
        this.lastError = null;
    }

    public void recordFailure(String error) {
        this.attempts++;
        this.lastError = (error != null && error.length() > MAX_ERROR_LENGTH)
                ? error.substring(0, MAX_ERROR_LENGTH)
                : error;
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