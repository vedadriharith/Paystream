package com.paystream.ledger.service;

import java.util.List;
import java.util.UUID;

import com.paystream.ledger.domain.LedgerAccount;
import com.paystream.ledger.domain.LedgerEntry;
import com.paystream.ledger.exception.LedgerAccountNotFoundException;
import com.paystream.ledger.repository.LedgerAccountRepository;
import com.paystream.ledger.repository.LedgerEntryRepository;
import com.paystream.ledger.web.dto.LedgerAccountResponse;
import com.paystream.ledger.web.dto.LedgerEntryResponse;
import com.paystream.ledger.web.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only queries over balances and journal entries. Entities are mapped to DTOs inside
 * the transaction, so lazy associations are never touched after it closes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerQueryService {

    private final LedgerAccountRepository accountRepository;
    private final LedgerEntryRepository entryRepository;

    public LedgerAccountResponse getAccount(String accountNumber) {
        return LedgerAccountResponse.from(findAccount(accountNumber));
    }

    public PageResponse<LedgerEntryResponse> getAccountEntries(String accountNumber, int page, int size) {
        // 404 for an unknown account instead of an empty page that looks like "no activity"
        findAccount(accountNumber);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LedgerEntry> entries = entryRepository.findByAccountNumber(accountNumber, pageable);

        log.debug("Listed {} of {} entries for a ledger account (page={}, size={})",
                entries.getNumberOfElements(), entries.getTotalElements(), page, size);
        return PageResponse.from(entries.map(LedgerEntryResponse::from));
    }

    public List<LedgerEntryResponse> getEntriesForPayment(UUID paymentId) {
        List<LedgerEntryResponse> entries = entryRepository.findByPaymentId(paymentId).stream()
                .map(LedgerEntryResponse::from)
                .toList();

        log.debug("Found {} ledger entries for payment {}", entries.size(), paymentId);
        return entries;
    }

    private LedgerAccount findAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new LedgerAccountNotFoundException(accountNumber));
    }
}