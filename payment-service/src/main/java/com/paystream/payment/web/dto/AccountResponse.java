package com.paystream.payment.web.dto;

import com.paystream.payment.domain.Account;

/**
 * Account details exposed to clients, e.g. for payer/payee dropdowns.
 */
public record AccountResponse(
        String accountNumber,
        String holderName,
        String currency
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getAccountNumber(), account.getHolderName(), account.getCurrency());
    }
}