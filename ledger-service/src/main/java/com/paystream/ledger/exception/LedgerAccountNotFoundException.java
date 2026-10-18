package com.paystream.ledger.exception;

public class LedgerAccountNotFoundException extends RuntimeException {

    public LedgerAccountNotFoundException(String accountNumber) {
        super("Ledger account " + accountNumber + " does not exist");
    }
}