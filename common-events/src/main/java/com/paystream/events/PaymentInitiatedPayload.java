package com.paystream.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Payload of {@link Topics#PAYMENTS_INITIATED}: a payment was accepted and needs to be applied to the ledger.
 */
public record PaymentInitiatedPayload(
        UUID paymentId,
        String payerAccount,
        String payeeAccount,
        BigDecimal amount,
        String currency,
        String reference
) {
}
