package com.paystream.payment.exception;

/**
 * Thrown when a well-formed request breaks a business rule (e.g. currency mismatch).
 */
public class PaymentValidationException extends RuntimeException {

    public PaymentValidationException(String message) {
        super(message);
    }
}