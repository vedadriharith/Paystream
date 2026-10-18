package com.paystream.payment.events;

import java.math.BigDecimal;
import java.util.UUID;

import com.paystream.events.EventEnvelope;
import com.paystream.events.PaymentInitiatedPayload;
import com.paystream.events.Topics;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that events survive a JSON round trip unchanged, which is what producers and consumers rely on.
 */
@Slf4j
class EventSerializationTest {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Test
    void paymentInitiatedEventSurvivesJsonRoundTrip() {
        UUID paymentId = UUID.randomUUID();
        PaymentInitiatedPayload payload = new PaymentInitiatedPayload(
                paymentId, "ACC1000000001", "ACC1000000002", new BigDecimal("250.00"), "INR", "Rent for October");
        EventEnvelope<PaymentInitiatedPayload> event =
                EventEnvelope.of(Topics.PAYMENTS_INITIATED, paymentId, payload);

        String json = jsonMapper.writeValueAsString(event);
        log.info("Serialized event: {}", json);

        EventEnvelope<PaymentInitiatedPayload> restored =
                jsonMapper.readValue(json, new TypeReference<EventEnvelope<PaymentInitiatedPayload>>() { });

        assertThat(restored).isEqualTo(event);
    }
}