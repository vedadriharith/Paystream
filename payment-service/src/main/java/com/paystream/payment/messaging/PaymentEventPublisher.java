package com.paystream.payment.messaging;

import com.paystream.events.EventEnvelope;
import com.paystream.events.PaymentInitiatedPayload;
import com.paystream.events.Topics;
import com.paystream.payment.web.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Publishes payment events to Kafka.
 *
 * <p>Phase 3 version: publishes directly after the database commit. This is a dual write and can lose
 * events if Kafka is unavailable; Phase 4 replaces it with the transactional outbox.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public void publishPaymentInitiated(PaymentResponse payment) {
        PaymentInitiatedPayload payload = new PaymentInitiatedPayload(
                payment.paymentId(),
                payment.payerAccount(),
                payment.payeeAccount(),
                payment.amount(),
                payment.currency(),
                payment.reference());
        EventEnvelope<PaymentInitiatedPayload> event =
                EventEnvelope.of(Topics.PAYMENTS_INITIATED, payment.paymentId(), payload);

        String key = payment.paymentId().toString();
        String json = jsonMapper.writeValueAsString(event);

        kafkaTemplate.send(Topics.PAYMENTS_INITIATED, key, json)
                .whenComplete((result, error) -> {
                    if (error !=null) {
                        log.error("Failed to publish event {} for payment {}", event.eventId(), key, error);
                        return;
                    }
                    log.info("Published event {} for payment {} to {} partition {} offset {}",
                            event.eventId(),
                            key,
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }
}
