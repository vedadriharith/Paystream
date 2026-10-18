package com.paystream.ledger.web.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.paystream.ledger.domain.AccountType;
import com.paystream.ledger.domain.LedgerAccount;

public record LedgerAccountResponse(
        String accountNumber,
        AccountType accountType,
        String currency,
        BigDecimal balance,
        Instant updatedAt
) {

    public static LedgerAccountResponse from(LedgerAccount account) {
        return new LedgerAccountResponse(
                account.getAccountNumber(),
                account.getAccountType(),
                account.getCurrency(),
                // Stored with 4 decimals; every amount has at most 2, so this never rounds
                account.getBalance().setScale(2, RoundingMode.UNNECESSARY),
                account.getUpdatedAt());
    }
}