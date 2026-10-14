-- Reference data for accounts known to payment-service.
-- Balances are NOT stored here; ledger-service owns balances.
CREATE TABLE accounts (
                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          account_number  VARCHAR(20)  NOT NULL,
                          holder_name     VARCHAR(120) NOT NULL,
                          currency        CHAR(3)      NOT NULL,
                          created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
                          CONSTRAINT uq_accounts_account_number UNIQUE (account_number),
                          CONSTRAINT ck_accounts_currency CHECK (currency ~ '^[A-Z]{3}$')
    );

-- One row per payment request. Status moves INITIATED -> COMPLETED | FAILED.
CREATE TABLE payments (
                          id               UUID PRIMARY KEY,
                          idempotency_key  VARCHAR(64)   NOT NULL,
                          payer_account    VARCHAR(20)   NOT NULL,
                          payee_account    VARCHAR(20)   NOT NULL,
                          amount           NUMERIC(19,4) NOT NULL,
                          currency         CHAR(3)       NOT NULL,
                          reference        VARCHAR(140),
                          status           VARCHAR(20)   NOT NULL,
                          failure_reason   VARCHAR(200),
                          created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
                          updated_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
                          CONSTRAINT uq_payments_idempotency_key UNIQUE (idempotency_key),
                          CONSTRAINT ck_payments_amount_positive CHECK (amount > 0),
                          CONSTRAINT ck_payments_status CHECK (status IN ('INITIATED', 'COMPLETED', 'FAILED')),
                          CONSTRAINT ck_payments_distinct_parties CHECK (payer_account <> payee_account),
                          CONSTRAINT fk_payments_payer FOREIGN KEY (payer_account) REFERENCES accounts (account_number),
                          CONSTRAINT fk_payments_payee FOREIGN KEY (payee_account) REFERENCES accounts (account_number)
);

-- Supports the dashboard query: filter by status, newest first
CREATE INDEX idx_payments_status_created ON payments (status, created_at DESC);
-- Supports "payments made by this account" lookups
CREATE INDEX idx_payments_payer ON payments (payer_account);