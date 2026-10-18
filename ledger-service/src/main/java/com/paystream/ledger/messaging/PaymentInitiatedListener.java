package com.paystream.ledger.messaging;

import com.paystream.events.EventEnvelope;
import com.paystream.events.PaymentInitiatedPayload;
import com.paystream.events.Topics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consumes payment initiated events. Phase 3 version only logs them;
 * Phase 5 applies double-entry bookkeeping.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentInitiatedListener {

    private static final TypeReference<EventEnvelope<PaymentInitiatedPayload>> EVENT_TYPE = new TypeReference<EventEnvelope<PaymentInitiatedPayload>>() { };

    private final JsonMapper jsonMapper;

    @KafkaListener(topics = Topics.PAYMENTS_INITIATED)
    public void onPaymentInitiated(ConsumerRecord<String, String> record) {
        EventEnvelope<PaymentInitiatedPayload> event = jsonMapper.readValue(record.value(), EVENT_TYPE);

        log.info("Received event {} for payment {} from partition {} offset {}: {} {}",
                event.eventId(),
                record.key(),
                record.partition(),
                record.offset(),
                event.payload().amount(),
                event.payload().currency());
    }
}
