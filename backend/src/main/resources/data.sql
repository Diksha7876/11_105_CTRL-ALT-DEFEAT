-- ============================================================
-- PaymentProcessing seed data
-- Executed by Spring Boot on startup via spring.sql.init.mode=always,
-- after schema.sql. Uses fixed UUIDs so rows are re-runnable/idempotent.
-- ============================================================

INSERT INTO bank_accounts (account_id, account_number, account_holder_name, payer_id, account_type, balance_in_inr, max_transaction_limit_in_inr, active)
VALUES
    ('11111111-1111-1111-1111-111111111101', 'ACC1000000001', 'Alice Johnson', '11111111-1111-1111-1111-111111111111', 'SAVINGS', 250000.00, 1000000.00, 1),
    ('11111111-1111-1111-1111-111111111102', 'ACC1000000002', 'Bob Smith',     '11111111-1111-1111-1111-111111111112', 'CURRENT', 500000.00, 2000000.00, 1),
    ('11111111-1111-1111-1111-111111111103', 'ACC1000000003', 'Carol Davis',  '11111111-1111-1111-1111-111111111113', 'SALARY',   75000.00,  500000.00, 1)
ON DUPLICATE KEY UPDATE account_id = account_id;

INSERT INTO beneficiaries (beneficiary_id, name, account_number, bank_name, ifsc_code, email, phone)
VALUES
    ('22222222-2222-2222-2222-222222222201', 'Daniel Green',  'ACC2000000001', 'HDFC Bank',    'HDFC0001234', 'daniel.green@example.com', '9876543210'),
    ('22222222-2222-2222-2222-222222222202', 'Emily Clarke',  'ACC2000000002', 'ICICI Bank',   'ICIC0005678', 'emily.clarke@example.com', '9876543211'),
    ('22222222-2222-2222-2222-222222222203', 'Frank Miller',  'ACC2000000003', 'Axis Bank',    'UTIB0009012', 'frank.miller@example.com', '9876543212')
ON DUPLICATE KEY UPDATE beneficiary_id = beneficiary_id;

INSERT INTO payments (payment_id, amount, currency, reference, status, version, payment_type, payment_method, card_type, payer_id, invoice_id, source_account_id, beneficiary_id, card_last4, card_holder_name, upi_id, idempotency_key, created_at, updated_at)
VALUES
    ('33333333-3333-3333-3333-333333333301', 1500.0000, 'INR', 'Electricity bill',   'COMPLETED', 0, 'BILL_PAYMENT',         'UPI',         NULL,           '11111111-1111-1111-1111-111111111111', 'INV-1001', '11111111-1111-1111-1111-111111111101', NULL,                                    NULL, NULL,             'alice.johnson@upi', 'idem-key-0001', NOW(6), NOW(6)),
    ('33333333-3333-3333-3333-333333333302', 25000.0000, 'INR', 'Transfer to Emily', 'COMPLETED', 0, 'BENEFICIARY_TRANSFER', 'NET_BANKING', NULL,           '11111111-1111-1111-1111-111111111112', 'INV-1002', '11111111-1111-1111-1111-111111111102', '22222222-2222-2222-2222-222222222202', NULL, NULL,             NULL,                'idem-key-0002', NOW(6), NOW(6)),
    ('33333333-3333-3333-3333-333333333303', 3200.5000, 'INR', 'Credit card bill',   'FAILED',    0, 'BILL_PAYMENT',         'CARD',        'CREDIT_CARD',  '11111111-1111-1111-1111-111111111113', 'INV-1003', '11111111-1111-1111-1111-111111111103', NULL,                                    '4242', 'Carol Davis',    NULL,                'idem-key-0003', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE payment_id = payment_id;

INSERT INTO payment_history (history_id, payment_id, old_status, new_status, timestamp, remarks, error_code, actor)
VALUES
    ('44444444-4444-4444-4444-444444444401', '33333333-3333-3333-3333-333333333301', NULL,        'CREATED',   NOW(6), 'Payment created',        NULL,      'SYSTEM'),
    ('44444444-4444-4444-4444-444444444402', '33333333-3333-3333-3333-333333333301', 'CREATED',   'COMPLETED', NOW(6), 'Payment completed',      NULL,      'SYSTEM'),
    ('44444444-4444-4444-4444-444444444403', '33333333-3333-3333-3333-333333333302', NULL,        'CREATED',   NOW(6), 'Payment created',        NULL,      'SYSTEM'),
    ('44444444-4444-4444-4444-444444444404', '33333333-3333-3333-3333-333333333302', 'CREATED',   'COMPLETED', NOW(6), 'Payment completed',      NULL,      'SYSTEM'),
    ('44444444-4444-4444-4444-444444444405', '33333333-3333-3333-3333-333333333303', NULL,        'CREATED',   NOW(6), 'Payment created',        NULL,      'SYSTEM'),
    ('44444444-4444-4444-4444-444444444406', '33333333-3333-3333-3333-333333333303', 'CREATED',   'FAILED',    NOW(6), 'Card declined by issuer','CARD_DECLINED', 'SYSTEM')
ON DUPLICATE KEY UPDATE history_id = history_id;
