package com.paystream.ledger.repository;

import java.util.Optional;
import java.util.UUID;

import com.paystream.ledger.domain.LedgerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerAccountRepository extends JpaRepository<LedgerAccount, UUID> {

    Optional<LedgerAccount> findByAccountNumber(String accountNumber);
}