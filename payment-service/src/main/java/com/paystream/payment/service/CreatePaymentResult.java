package com.paystream.payment.service;

import com.paystream.payment.web.dto.PaymentResponse;

/**
 * Outcome of a create-payment call: either a newly created payment or a replay of an earlier one.
 */
public record CreatePaymentResult(PaymentResponse payment, boolean created) {

    static CreatePaymentResult created(PaymentResponse payment) {
        return new CreatePaymentResult(payment, true);
    }

    static CreatePaymentResult replayed(PaymentResponse payment) {
        return new CreatePaymentResult(payment, false);
    }
}
