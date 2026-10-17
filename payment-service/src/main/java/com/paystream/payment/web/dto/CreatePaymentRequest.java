package com.paystream.payment.web.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request body for creating a payment. Validates the shape of the request only;
 * business rules (accounts exist, currencies match) are enforced in the service layer.
 */
public record CreatePaymentRequest(

        @NotBlank
        @Size(max = 20)
        String payerAccount,

        @NotBlank
        @Size(max = 20)
        String payeeAccount,

        @NotNull
        @Positive
        @Digits(integer = 15, fraction = 2)
        BigDecimal amount,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter uppercase ISO currency code")
        String currency,

        @Size(max = 140)
        String reference
) {
}