package com.paystream.payment.exception;

public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException(String accountNumber) {
        super("Account " + accountNumber + " does not exist");
    }
}