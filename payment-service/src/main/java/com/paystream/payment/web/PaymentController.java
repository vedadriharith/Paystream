package com.paystream.payment.web;

import java.net.URI;
import java.util.UUID;

import com.paystream.payment.domain.PaymentStatus;
import com.paystream.payment.service.CreatePaymentResult;
import com.paystream.payment.service.PaymentService;
import com.paystream.payment.web.dto.CreatePaymentRequest;
import com.paystream.payment.web.dto.PageResponse;
import com.paystream.payment.web.dto.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * HTTP layer for payments. Handles only request/response concerns; all rules live in {@link PaymentService}.
 */
@Tag(name = "Payments", description = "Create payments and track their status")
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create a payment",
            description = "Returns 201 for a new payment. Retrying with the same Idempotency-Key and body "
                    + "returns the original payment with 200; reusing the key with a different body returns 409.")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "^[A-Za-z0-9_-]{8,64}$",
                    message = "Idempotency-Key must be 8-64 characters of letters, digits, '-' or '_'")
            String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {

        CreatePaymentResult result = paymentService.createPayment(idempotencyKey, request);

        if (!result.created()) {
            return ResponseEntity.status(HttpStatus.OK)
                    .header("Idempotent-Replayed", "true")
                    .body(result.payment());
        }

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{paymentId}")
                .buildAndExpand(result.payment().paymentId())
                .toUri();
        return ResponseEntity.created(location).body(result.payment());
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get a payment by id")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        return paymentService.getPayment(paymentId);
    }

    @GetMapping
    @Operation(summary = "List payments, newest first, optionally filtered by status")
    public PageResponse<PaymentResponse> listPayments(
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be 0 or greater") int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be at most 100") int size) {
        return paymentService.listPayments(status, page, size);
    }
}