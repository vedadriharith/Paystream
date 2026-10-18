package com.paystream.payment.messaging;

import com.paystream.events.EventEnvelope;
import com.paystream.events.PaymentInitiatedPayload;
import com.paystream.events.Topics;
import com.paystream.payment.domain.OutboxEvent;
import com.paystream.payment.domain.Payment;
import com.paystream.payment.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Records outgoing events in the outbox table. Must run inside the caller's transaction
 * so the event is committed atomically with the business change.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxWriter {

    private static final String AGGREGATE_PAYMENT = "Payment";

    private final OutboxEventRepository  outboxEventRepository;
    private final JsonMapper jsonMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void paymentInitiated(Payment payment) {
        PaymentInitiatedPayload payload = new PaymentInitiatedPayload(
                payment.getId(),
                payment.getPayerAccount(),
                payment.getPayeeAccount(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference());
        EventEnvelope<PaymentInitiatedPayload> event =
                EventEnvelope.of(Topics.PAYMENTS_INITIATED, payment.getId(), payload);

        outboxEventRepository.save(OutboxEvent.pending(
                event.eventId(),
                AGGREGATE_PAYMENT,
                payment.getId(),
                event.eventType(),
                jsonMapper.writeValueAsString(event)));

        log.info("Queued event {} ({}) for payment {} in outbox",
                event.eventId(), event.eventType(), payment.getId());
    }
}
