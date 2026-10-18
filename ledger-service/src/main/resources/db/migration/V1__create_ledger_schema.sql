-- Accounts as the ledger sees them. The ledger is the only source of truth for balances.
-- CUSTOMER accounts mirror payment-service accounts; SYSTEM accounts are internal (e.g. funding).
CREATE TABLE ledger_accounts (
                                 id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
                                 account_number  VARCHAR(20)   NOT NULL,
                                 account_type    VARCHAR(20)   NOT NULL,
                                 currency        CHAR(3)       NOT NULL,
                                 balance         NUMERIC(19,4) NOT NULL DEFAULT 0,
                                 version         BIGINT        NOT NULL DEFAULT 0,
                                 created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                 updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                 CONSTRAINT uq_ledger_accounts_account_number UNIQUE (account_number),
    -- Target for the composite foreign key from ledger_entries (entry currency must match account currency)
                                 CONSTRAINT uq_ledger_accounts_number_currency UNIQUE (account_number, currency),
                                 CONSTRAINT ck_ledger_accounts_type CHECK (account_type IN ('CUSTOMER', 'SYSTEM')),
                                 CONSTRAINT ck_ledger_accounts_currency CHECK (currency ~ '^[A-Z]{3}$'),
    -- Customer balances can never go negative; system accounts can (they represent money from outside)
    CONSTRAINT ck_ledger_accounts_no_overdraft CHECK (account_type = 'SYSTEM' OR balance >= 0)
);

-- One business event that moves money. Its entries must balance: total debits = total credits.
CREATE TABLE ledger_transactions (
                                     id           UUID          PRIMARY KEY,
                                     type         VARCHAR(30)   NOT NULL,
                                     payment_id   UUID,
                                     description  VARCHAR(200),
                                     created_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                     CONSTRAINT ck_ledger_transactions_type CHECK (type IN ('OPENING_BALANCE', 'PAYMENT')),
    -- PAYMENT transactions must reference a payment; other types must not
                                     CONSTRAINT ck_ledger_transactions_payment_ref CHECK ((type = 'PAYMENT') = (payment_id IS NOT NULL)),
    -- A payment can be posted at most once, even if it arrives under different event ids
                                     CONSTRAINT uq_ledger_transactions_payment UNIQUE (payment_id)
);

-- Append-only: entries are never updated or deleted; corrections are new reversing transactions.
CREATE TABLE ledger_entries (
                                id              UUID          PRIMARY KEY,
                                transaction_id  UUID          NOT NULL,
                                account_number  VARCHAR(20)   NOT NULL,
                                direction       VARCHAR(6)    NOT NULL,
                                amount          NUMERIC(19,4) NOT NULL,
                                currency        CHAR(3)       NOT NULL,
                                created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                CONSTRAINT ck_ledger_entries_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
                                CONSTRAINT ck_ledger_entries_amount_positive CHECK (amount > 0),
                                CONSTRAINT fk_ledger_entries_transaction FOREIGN KEY (transaction_id) REFERENCES ledger_transactions (id),
                                CONSTRAINT fk_ledger_entries_account FOREIGN KEY (account_number, currency)
                                    REFERENCES ledger_accounts (account_number, currency)
);

-- Postgres does not index foreign key columns automatically
CREATE INDEX idx_ledger_entries_transaction ON ledger_entries (transaction_id);
-- Account statement: entries for one account, newest first
CREATE INDEX idx_ledger_entries_account_created ON ledger_entries (account_number, created_at DESC);

-- Idempotent consumer guard: one row per (event, consumer) that has been fully processed.
CREATE TABLE processed_events (
                                  event_id      UUID          NOT NULL,
                                  consumer      VARCHAR(60)   NOT NULL,
                                  processed_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
                                  CONSTRAINT pk_processed_events PRIMARY KEY (event_id, consumer)
);