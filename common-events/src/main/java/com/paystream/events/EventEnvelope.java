package com.paystream.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Common wrapper for every event published to Kafka.
 *
 * @param eventId     unique per event; consumers use it to skip duplicates
 * @param eventType   versioned event name, equal to the topic it is published on
 * @param occurredAt  when the business event happened
 * @param aggregateId id of the entity the event is about; also used as the Kafka message key
 * @param payload     event-specific data
 */
public record EventEnvelope<T> (
    UUID eventId,
    String eventType,
    Instant occurredAt,
    UUID aggregateId,
    T payload
) {

    public static <T> EventEnvelope<T> of(String eventType, UUID aggregateId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, Instant.now(), aggregateId, payload);
    }
}
