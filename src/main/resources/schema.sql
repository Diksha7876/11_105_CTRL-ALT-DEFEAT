-- ============================================================
-- PaymentProcessing MySQL Schema
-- This file is executed by Spring Boot on startup via
-- spring.sql.init.mode=always (safe due to IF NOT EXISTS).
-- ============================================================

CREATE TABLE IF NOT EXISTS bank_accounts (
    account_id          CHAR(36)        NOT NULL,
    account_number      VARCHAR(34)     NOT NULL,
    account_holder_name VARCHAR(255)    NOT NULL,
    active              TINYINT(1)      NOT NULL DEFAULT 1,
    PRIMARY KEY (account_id),
    UNIQUE KEY uk_bank_accounts_account_number (account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS beneficiaries (
    beneficiary_id  CHAR(36)        NOT NULL,
    name            VARCHAR(255)    NOT NULL,
    account_number  VARCHAR(34)     NOT NULL,
    bank_name       VARCHAR(255)    NOT NULL,
    ifsc_code       VARCHAR(11)     NOT NULL,
    email           VARCHAR(255)    NOT NULL,
    phone           VARCHAR(20)     NULL,
    PRIMARY KEY (beneficiary_id),
    UNIQUE KEY uk_beneficiaries_account_number (account_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payments (
    payment_id      CHAR(36)        NOT NULL,
    amount          DECIMAL(19,4)   NOT NULL,
    currency        VARCHAR(3)      NOT NULL,
    reference       VARCHAR(255)    NOT NULL,
    status          VARCHAR(50)     NOT NULL COMMENT 'CREATED | VALIDATED | SENT | COMPLETED | FAILED',
    version         BIGINT          NULL,
    payment_type    VARCHAR(50)     NOT NULL COMMENT 'BILL_PAYMENT | BENEFICIARY_TRANSFER',
    payer_id        CHAR(36)        NOT NULL,
    invoice_id      VARCHAR(255)    NULL,
    source_account_id CHAR(36)      NOT NULL,
    beneficiary_id  CHAR(36)        NOT NULL,
    idempotency_key VARCHAR(255)    NOT NULL,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    PRIMARY KEY (payment_id),
    UNIQUE KEY uk_payments_idempotency_key (idempotency_key),
    UNIQUE KEY uk_payment_payer_invoice (payer_id, invoice_id),
    CONSTRAINT fk_payments_source_account
        FOREIGN KEY (source_account_id) REFERENCES bank_accounts (account_id),
    CONSTRAINT fk_payments_beneficiary
        FOREIGN KEY (beneficiary_id)    REFERENCES beneficiaries (beneficiary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS payment_history (
    history_id  CHAR(36)        NOT NULL,
    payment_id  CHAR(36)        NOT NULL,
    old_status  VARCHAR(50)     NULL     COMMENT 'NULL on first transition',
    new_status  VARCHAR(50)     NOT NULL,
    timestamp   DATETIME(6)     NOT NULL,
    remarks     VARCHAR(500)    NULL,
    error_code  VARCHAR(100)    NULL,
    actor       VARCHAR(255)    NOT NULL,
    PRIMARY KEY (history_id),
    CONSTRAINT fk_payment_history_payment
        FOREIGN KEY (payment_id) REFERENCES payments (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
