package com.paystream.payment.repository;

import java.math.BigDecimal;
import java.util.Optional;

import com.paystream.payment.domain.Account;
import com.paystream.payment.domain.Payment;
import com.paystream.payment.domain.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies entity mappings and repository queries against the local payment_db.
 * Each test runs in a transaction that is rolled back, so no data is left behind.
 */
@Slf4j
@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "logging.level.org.hibernate.SQL=DEBUG"
})
@Transactional
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void seededAccountsAreReadable() {
        Optional<Account> account = accountRepository.findByAccountNumber("ACC1000000001");

        assertThat(account).isPresent();
        assertThat(account.get().getCurrency()).isEqualTo("INR");
        log.info("Loaded seeded account {} ({})", account.get().getAccountNumber(), account.get().getHolderName());
    }

    @Test
    void savesPaymentAndFindsItByIdempotencyKey() {
        Payment payment = Payment.initiate("test-key-1", "ACC1000000001", "ACC1000000002",
                new BigDecimal("250.00"), "INR", "Rent for October");

        paymentRepository.saveAndFlush(payment);
        Optional<Payment> found = paymentRepository.findByIdempotencyKey("test-key-1");

        assertThat(found).isPresent();
        assertThat(found.get().getStatus()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(found.get().getAmount()).isEqualByComparingTo("250.00");
        assertThat(found.get().getCreatedAt()).isNotNull();
        log.info("Saved payment {} with status {}", found.get().getId(), found.get().getStatus());
    }

    @Test
    void completedPaymentCannotBeMarkedFailed() {
        Payment payment = Payment.initiate("test-key-2", "ACC1000000001", "ACC1000000002",
                new BigDecimal("100.00"), "INR", null);
        payment.markCompleted();

        assertThatThrownBy(() -> payment.markFailed("late failure"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be INITIATED");
    }
}