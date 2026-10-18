package com.paystream.ledger.domain;

/**
 * CUSTOMER balances may never go negative; SYSTEM accounts (such as funding) may.
 */
public enum AccountType {
    CUSTOMER,
    SYSTEM
}
