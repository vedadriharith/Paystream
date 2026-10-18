package com.paystream.payment.service;

import java.util.UUID;

import com.paystream.payment.domain.Account;
import com.paystream.payment.domain.Payment;
import com.paystream.payment.domain.PaymentStatus;
import com.paystream.payment.exception.AccountNotFoundException;
import com.paystream.payment.exception.IdempotencyKeyReuseException;
import com.paystream.payment.exception.PaymentNotFoundException;
import com.paystream.payment.exception.PaymentValidationException;
import com.paystream.payment.messaging.OutboxWriter;
import com.paystream.payment.repository.AccountRepository;
import com.paystream.payment.repository.PaymentRepository;
import com.paystream.payment.web.dto.CreatePaymentRequest;
import com.paystream.payment.web.dto.PageResponse;
import com.paystream.payment.web.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Business logic for accepting and reading payments. Entities never leave this layer;
 * callers receive {@link PaymentResponse} DTOs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final String IDEMPOTENCY_KEY_CONSTRAINT = "uq_payments_idempotency_key";

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;
    private final TransactionTemplate transactionTemplate;
    private final OutboxWriter  outboxWriter;

    /**
     * Creates a payment, or replays the original one if this Idempotency-Key was seen before.
     * Deliberately not @Transactional: a failed insert aborts its transaction, so the
     * concurrent-duplicate path must read the winning row in a separate transaction.
     */
    public CreatePaymentResult createPayment(String idempotencyKey, CreatePaymentRequest request) {
        String requestHash = PaymentRequestHasher.hash(request);

        // Fast path: an ordinary client retry finds the existing payment without touching constraints
        var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return replay(existing.get(), idempotencyKey, requestHash);
        }

        try {
            PaymentResponse created = transactionTemplate.execute(
                    status -> insertNewPayment(idempotencyKey, requestHash, request));
            return CreatePaymentResult.created(created);
        } catch (DataIntegrityViolationException ex) {
            if (!isIdempotencyKeyViolation(ex)) {
                throw ex;
            }
            // Race: a concurrent request with the same key committed between our lookup and our insert
            log.info("Concurrent request already created a payment for idempotency key {}; replaying it",
                    idempotencyKey);
            Payment winner = paymentRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> ex);
            return replay(winner, idempotencyKey, requestHash);
        }
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> listPayments(PaymentStatus status, int page, int size) {
        // Fixed sort keeps queries on the (status, created_at DESC) index and avoids client-chosen sort fields
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Payment> result = (status == null)
                ? paymentRepository.findAll(pageable)
                : paymentRepository.findByStatus(status, pageable);

        log.debug("Listed {} of {} payments (status={}, page={}, size={})",
                result.getNumberOfElements(), result.getTotalElements(), status, page, size);
        return PageResponse.from(result.map(PaymentResponse::from));
    }

    private PaymentResponse insertNewPayment(String idempotencyKey, String requestHash, CreatePaymentRequest request) {
        validateParties(request);

        Payment payment = Payment.initiate(
                idempotencyKey,
                requestHash,
                request.payerAccount(),
                request.payeeAccount(),
                request.amount(),
                request.currency(),
                request.reference());
        Payment saved = paymentRepository.saveAndFlush(payment);
        outboxWriter.paymentInitiated(saved);

        log.info("Payment {} initiated: {} -> {} for {} {}",
                saved.getId(),
                mask(saved.getPayerAccount()),
                mask(saved.getPayeeAccount()),
                saved.getAmount(),
                saved.getCurrency());
        return PaymentResponse.from(saved);
    }

    private CreatePaymentResult replay(Payment existing, String idempotencyKey, String requestHash) {
        if (!requestHash.equals(existing.getRequestHash())) {
            throw new IdempotencyKeyReuseException(idempotencyKey);
        }
        log.info("Replaying payment {} for idempotency key {}", existing.getId(), idempotencyKey);
        return CreatePaymentResult.replayed(PaymentResponse.from(existing));
    }

    private static boolean isIdempotencyKeyViolation(DataIntegrityViolationException ex) {
        Throwable cause = ex;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return IDEMPOTENCY_KEY_CONSTRAINT.equals(violation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void validateParties(CreatePaymentRequest request) {
        if (request.payerAccount().equals(request.payeeAccount())) {
            throw new PaymentValidationException("Payer and payee accounts must be different");
        }

        Account payer = findAccount(request.payerAccount());
        Account payee = findAccount(request.payeeAccount());

        if (!payer.getCurrency().equals(request.currency()) || !payee.getCurrency().equals(request.currency())) {
            throw new PaymentValidationException(
                    "Payment currency %s does not match account currencies (payer %s, payee %s)"
                            .formatted(request.currency(), payer.getCurrency(), payee.getCurrency()));
        }
    }

    private Account findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }

    /** Masks all but the last 4 characters so full account numbers never reach the logs. */
    private static String mask(String accountNumber) {
        int visible = 4;
        if (accountNumber.length() <= visible) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - visible);
    }
}