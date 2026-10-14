-- Seed accounts for local development and demos.
-- Matching balances are seeded separately in ledger-service.
INSERT INTO accounts (account_number, holder_name, currency) VALUES
                                                                 ('ACC1000000001', 'Ananya Rao',      'INR'),
                                                                 ('ACC1000000002', 'Vikram Reddy',    'INR'),
                                                                 ('ACC1000000003', 'Meera Iyer',      'INR'),
                                                                 ('ACC1000000004', 'Rahul Varma',     'INR'),
                                                                 ('ACC1000000005', 'Global Imports',  'USD');