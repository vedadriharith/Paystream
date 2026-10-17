package com.paystream.payment.web.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import com.paystream.payment.domain.Payment;
import com.paystream.payment.domain.PaymentStatus;

/**
 * API representation of a payment. Decouples the public contract from the JPA entity.
 */
public record PaymentResponse(
        UUID paymentId,
        String payerAccount,
        String payeeAccount,
        BigDecimal amount,
        String currency,
        String reference,
        PaymentStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPayerAccount(),
                payment.getPayeeAccount(),
                // Input is validated to at most 2 decimals, so this never rounds
                payment.getAmount().setScale(2, RoundingMode.UNNECESSARY),
                payment.getCurrency(),
                payment.getReference(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}