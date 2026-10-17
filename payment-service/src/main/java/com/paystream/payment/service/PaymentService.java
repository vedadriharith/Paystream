package com.paystream.payment.service;

import com.paystream.payment.domain.Account;
import com.paystream.payment.domain.Payment;
import com.paystream.payment.exception.AccountNotFoundException;
import com.paystream.payment.exception.PaymentNotFoundException;
import com.paystream.payment.exception.PaymentValidationException;
import com.paystream.payment.repository.AccountRepository;
import com.paystream.payment.repository.PaymentRepository;
import com.paystream.payment.web.dto.CreatePaymentRequest;
import com.paystream.payment.web.dto.PaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Business logic for accepting and reading payments. Entities never leave this layer;
 * callers receive {@link PaymentResponse} DTOs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public PaymentResponse createPayment(String idempotencyKey, CreatePaymentRequest request) {
        validateParties(request);

        Payment payment = Payment.initiate(
                idempotencyKey,
                request.payerAccount(),
                request.payeeAccount(),
                request.amount(),
                request.currency(),
                request.reference());
        Payment saved = paymentRepository.saveAndFlush(payment);

        log.info("Payment {} initiated: {} -> {} for {} {}",
                saved.getId(),
                mask(saved.getPayerAccount()),
                mask(saved.getPayeeAccount()),
                saved.getAmount(),
                saved.getCurrency());
        return PaymentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }

    private void validateParties(CreatePaymentRequest request) {
        if (request.payerAccount().equals(request.payeeAccount())) {
            throw new PaymentValidationException("Payer and Payee accounts must be different");
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
