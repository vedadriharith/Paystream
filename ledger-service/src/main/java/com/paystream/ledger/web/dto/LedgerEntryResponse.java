package com.paystream.ledger.web.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import com.paystream.ledger.domain.EntryDirection;
import com.paystream.ledger.domain.LedgerEntry;

public record LedgerEntryResponse(
        UUID entryId,
        UUID transactionId,
        String accountNumber,
        EntryDirection direction,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {

    public static LedgerEntryResponse from(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransaction().getId(),
                entry.getAccountNumber(),
                entry.getDirection(),
                entry.getAmount().setScale(2, RoundingMode.UNNECESSARY),
                entry.getCurrency(),
                entry.getCreatedAt());
    }
}