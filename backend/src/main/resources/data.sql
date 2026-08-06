-- ============================================================
-- PaymentProcessing seed data
-- Executed by Spring Boot on startup via spring.sql.init.mode=always,
-- after schema.sql. Uses fixed UUIDs so rows are re-runnable/idempotent.
-- ============================================================

INSERT INTO bank_accounts (account_id, account_number, account_holder_name, payer_id, account_type, balance_in_inr, max_transaction_limit_in_inr, active)
VALUES
    ('111111101', 'ACC1000000001', 'Alice Johnson', '111111111', 'SAVINGS', 250000.00, 1000000.00, 1),
    ('111111102', 'ACC1000000002', 'Bob Smith',     '111111112', 'CURRENT', 500000.00, 2000000.00, 1),
    ('111111103', 'ACC1000000003', 'Carol Davis',  '111111113', 'SALARY',   75000.00,  500000.00, 1)
ON DUPLICATE KEY UPDATE account_id = account_id;

INSERT INTO beneficiaries (beneficiary_id, name, account_number, bank_name, ifsc_code, email, phone)
VALUES
    ('222222201', 'Daniel Green',  'ACC2000000001', 'HDFC Bank',    'HDFC0001234', 'daniel.green@example.com', '9876543210'),
    ('222222202', 'Emily Clarke',  'ACC2000000002', 'ICICI Bank',   'ICIC0005678', 'emily.clarke@example.com', '9876543211'),
    ('222222203', 'Frank Miller',  'ACC2000000003', 'Axis Bank',    'UTIB0009012', 'frank.miller@example.com', '9876543212')
ON DUPLICATE KEY UPDATE beneficiary_id = beneficiary_id;

INSERT INTO payments (payment_id, amount, currency, reference, status, version, payment_type, payment_method, card_type, payer_id, invoice_id, source_account_id, beneficiary_id, card_last4, card_holder_name, upi_id, idempotency_key, created_at, updated_at)
VALUES
    ('333333301', 1500.0000, 'INR', 'Electricity bill',   'COMPLETED', 0, 'BILL_PAYMENT',         'UPI',         NULL,           '111111111', 'INV-1001', '111111101', NULL,                                    NULL, NULL,             'alice.johnson@upi', 'idem-key-0001', NOW(6), NOW(6)),
    ('333333302', 25000.0000, 'INR', 'Transfer to Emily', 'COMPLETED', 0, 'BENEFICIARY_TRANSFER', 'NET_BANKING', NULL,           '111111112', 'INV-1002', '111111102', '222222202', NULL, NULL,             NULL,                'idem-key-0002', NOW(6), NOW(6)),
    ('333333303', 3200.5000, 'INR', 'Credit card bill',   'FAILED',    0, 'BILL_PAYMENT',         'CARD',        'CREDIT_CARD',  '111111113', 'INV-1003', '111111103', NULL,                                    '4242', 'Carol Davis',    NULL,                'idem-key-0003', NOW(6), NOW(6))
ON DUPLICATE KEY UPDATE payment_id = payment_id;

INSERT INTO payment_history (history_id, payment_id, old_status, new_status, timestamp, remarks, error_code, actor)
SELECT '444444401', '333333301', NULL,        'CREATED',   NOW(6), 'Payment created',        NULL,            'SYSTEM'
FROM payments p WHERE p.payment_id = '333333301'
UNION ALL
SELECT '444444402', '333333301', 'CREATED',   'COMPLETED', NOW(6), 'Payment completed',      NULL,            'SYSTEM'
FROM payments p WHERE p.payment_id = '333333301'
UNION ALL
SELECT '444444403', '333333302', NULL,        'CREATED',   NOW(6), 'Payment created',        NULL,            'SYSTEM'
FROM payments p WHERE p.payment_id = '333333302'
UNION ALL
SELECT '444444404', '333333302', 'CREATED',   'COMPLETED', NOW(6), 'Payment completed',      NULL,            'SYSTEM'
FROM payments p WHERE p.payment_id = '333333302'
UNION ALL
SELECT '444444405', '333333303', NULL,        'CREATED',   NOW(6), 'Payment created',        NULL,            'SYSTEM'
FROM payments p WHERE p.payment_id = '333333303'
UNION ALL
SELECT '444444406', '333333303', 'CREATED',   'FAILED',    NOW(6), 'Card declined by issuer','CARD_DECLINED', 'SYSTEM'
FROM payments p WHERE p.payment_id = '333333303'
ON DUPLICATE KEY UPDATE history_id = history_id;
