package com.paystream.ledger.repository;

import java.util.List;
import java.util.UUID;

import com.paystream.ledger.domain.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    /** Entries of the payment's ledger transaction, DEBIT before CREDIT. */
    @Query("""
            select e from LedgerEntry e
            join fetch e.transaction t
            where t.paymentId = :paymentId
            order by e.createdAt, e.direction desc
            """)
    List<LedgerEntry> findByPaymentId(@Param("paymentId") UUID paymentId);

    /** Account statement; served by idx_ledger_entries_account_created. */
    Page<LedgerEntry> findByAccountNumber(String accountNumber, Pageable pageable);
}