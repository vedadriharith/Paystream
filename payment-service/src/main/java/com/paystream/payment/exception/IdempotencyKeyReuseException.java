package com.paystream.payment.exception;

/**
 * Thrown when an Idempotency-Key that was already used is sent again with a different request body.
 */
public class IdempotencyKeyReuseException extends RuntimeException {

    public IdempotencyKeyReuseException(String idempotencyKey) {
        super("Idempotency-Key " + idempotencyKey + " was already used with a different request body");
    }
}