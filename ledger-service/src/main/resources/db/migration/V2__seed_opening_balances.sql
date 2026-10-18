-- Opening balances for local development and demos.
-- Money enters through a SYSTEM funding account per currency, so the books balance from day one:
-- each funding account goes negative by exactly the amount its customers receive.
-- Customer account numbers must match payment-service's V2__seed_accounts.sql.

INSERT INTO ledger_accounts (account_number, account_type, currency) VALUES
                                                                         ('SYS-FUNDING-INR', 'SYSTEM',   'INR'),
                                                                         ('SYS-FUNDING-USD', 'SYSTEM',   'USD'),
                                                                         ('ACC1000000001',   'CUSTOMER', 'INR'),
                                                                         ('ACC1000000002',   'CUSTOMER', 'INR'),
                                                                         ('ACC1000000003',   'CUSTOMER', 'INR'),
                                                                         ('ACC1000000004',   'CUSTOMER', 'INR'),
                                                                         ('ACC1000000005',   'CUSTOMER', 'USD');

-- One OPENING_BALANCE transaction per customer account; fixed ids keep the seed deterministic
INSERT INTO ledger_transactions (id, type, description) VALUES
                                                            ('00000000-0000-0000-0000-000000000001', 'OPENING_BALANCE', 'Opening balance ACC1000000001'),
                                                            ('00000000-0000-0000-0000-000000000002', 'OPENING_BALANCE', 'Opening balance ACC1000000002'),
                                                            ('00000000-0000-0000-0000-000000000003', 'OPENING_BALANCE', 'Opening balance ACC1000000003'),
                                                            ('00000000-0000-0000-0000-000000000004', 'OPENING_BALANCE', 'Opening balance ACC1000000004'),
                                                            ('00000000-0000-0000-0000-000000000005', 'OPENING_BALANCE', 'Opening balance ACC1000000005');

-- Each opening transaction: DEBIT the funding account, CREDIT the customer, same amount.
-- ACC1000000004 gets a small balance so the insufficient-funds path is easy to demo.
WITH opening (txn_id, customer_account, currency, amount) AS (
    VALUES
        ('00000000-0000-0000-0000-000000000001'::uuid, 'ACC1000000001', 'INR', 100000.00),
        ('00000000-0000-0000-0000-000000000002'::uuid, 'ACC1000000002', 'INR',  50000.00),
        ('00000000-0000-0000-0000-000000000003'::uuid, 'ACC1000000003', 'INR',  25000.00),
        ('00000000-0000-0000-0000-000000000004'::uuid, 'ACC1000000004', 'INR',   1000.00),
        ('00000000-0000-0000-0000-000000000005'::uuid, 'ACC1000000005', 'USD',  10000.00)
)
INSERT INTO ledger_entries (id, transaction_id, account_number, direction, amount, currency)
SELECT gen_random_uuid(), txn_id, 'SYS-FUNDING-' || currency, 'DEBIT', amount, currency FROM opening
UNION ALL
SELECT gen_random_uuid(), txn_id, customer_account, 'CREDIT', amount, currency FROM opening;

-- Balances are derived from the entries, so they cannot disagree with the journal
UPDATE ledger_accounts a
SET balance = COALESCE((
                           SELECT SUM(CASE WHEN e.direction = 'CREDIT' THEN e.amount ELSE -e.amount END)
                           FROM ledger_entries e
                           WHERE e.account_number = a.account_number
                       ), 0);