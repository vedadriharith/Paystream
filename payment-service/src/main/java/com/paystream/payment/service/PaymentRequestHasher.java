package com.paystream.payment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import com.paystream.payment.web.dto.CreatePaymentRequest;
import lombok.experimental.UtilityClass;

/**
 * Produces a stable fingerprint of a create-payment request so a retried request
 * can be told apart from a different request that reuses the same Idempotency-Key.
 */
@UtilityClass
public class PaymentRequestHasher {

    /** Hex-encoded SHA-256 of the request's business fields. */
    public String hash(CreatePaymentRequest request) {
        StringBuilder canonical = new StringBuilder();
        append(canonical, request.payerAccount());
        append(canonical, request.payeeAccount());
        // 250, 250.0 and 250.00 are the same amount and must hash identically
        append(canonical, request.amount().stripTrailingZeros().toPlainString());
        append(canonical, request.currency());
        append(canonical, request.reference() == null ? "" : request.reference());
        return sha256Hex(canonical.toString());
    }

    /** Length-prefixing makes the encoding unambiguous: ("ab","c") and ("a","bc") produce different input. */
    private void append(StringBuilder builder, String value) {
        builder.append(value.length()).append(':').append(value).append(';');
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", e);
        }
    }
}