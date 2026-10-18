package com.paystream.payment.messaging;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.paystream.payment.config.OutboxProperties;
import com.paystream.payment.domain.OutboxEvent;
import com.paystream.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Relays committed outbox events to Kafka.
 *
 * <p>Delivery is at-least-once: if the process dies after Kafka acknowledges a send but before
 * published_at is committed, the event is sent again on the next poll. Consumers must deduplicate
 * by eventId.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "paystream.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;
    private final OutboxProperties properties;

    @Scheduled(fixedDelayString = "${paystream.outbox.poll-interval-ms:500}")
    public void publishPending() {
        Integer published = transactionTemplate.execute(status -> publishBatch());
        if (published != null && published > 0) {
            log.debug("Outbox poll published {} event(s)", published);
        }
    }

    private int publishBatch() {
        List<OutboxEvent> batch = outboxEventRepository.lockNextBatch(properties.batchSize());
        int published = 0;

        for (OutboxEvent event : batch) {
            try {
                // The event type is the topic name, and the aggregate id is the key, so all events
                // for one payment land on the same partition in order
                SendResult<String, String> result = kafkaTemplate
                        .send(event.getEventType(), event.getAggregateId().toString(), event.getPayload())
                        .get(properties.sendTimeoutMs(), TimeUnit.MILLISECONDS);

                event.markPublished(Instant.now());
                published++;
                log.info("Published outbox event {} for {} {} to {} partition {} offset {}",
                        event.getId(),
                        event.getAggregateType(),
                        event.getAggregateId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                event.recordFailure("Interrupted while waiting for Kafka acknowledgement");
                break;
            } catch (Exception ex) {
                Throwable cause = (ex instanceof ExecutionException && ex.getCause() != null) ? ex.getCause() : ex;
                event.recordFailure(cause.toString());
                logFailure(event, cause);
                // Stop here: later events in the batch may belong to the same payment and must not overtake this one
                break;
            }
        }
        return published;
    }

    private void logFailure(OutboxEvent event, Throwable cause) {
        if (event.getAttempts() >= properties.alertAfterAttempts()) {
            log.error("Outbox event {} still unpublished after {} attempts: {}",
                    event.getId(), event.getAttempts(), cause.toString());
        } else {
            log.warn("Publishing outbox event {} failed (attempt {}): {}",
                    event.getId(), event.getAttempts(), cause.toString());
        }
    }
}