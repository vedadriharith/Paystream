package com.paystream.payment.web;

import com.paystream.payment.service.PaymentService;
import com.paystream.payment.web.dto.CreatePaymentRequest;
import com.paystream.payment.web.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * HTTP layer for payments. Handles only request/response concerns; all rules live in {@link PaymentService}.
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                                         @Valid @RequestBody CreatePaymentRequest request) {
        PaymentResponse response = paymentService.createPayment(idempotencyKey, request);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{paymentId}")
                .buildAndExpand(response.paymentId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse getPayment(@PathVariable UUID paymentId) {
        return paymentService.getPayment(paymentId);
    }
}
