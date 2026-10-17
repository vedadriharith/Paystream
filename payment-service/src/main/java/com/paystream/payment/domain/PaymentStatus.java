package com.paystream.payment.domain;

/**
 * Lifecycle of a payment. A payment starts INITIATED and ends in exactly one terminal state.
 */
public enum PaymentStatus {
    INITIATED,
    COMPLETED,
    FAILED
}
