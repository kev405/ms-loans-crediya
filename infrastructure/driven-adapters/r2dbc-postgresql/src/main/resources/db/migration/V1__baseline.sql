CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =========================
-- estados (StateLoanEntity)
-- =========================
CREATE TABLE IF NOT EXISTS loan_state (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(80)  NOT NULL UNIQUE,
    description     VARCHAR(255)
    );

-- =========================
-- loan_type (TypeLoanEntity)
-- =========================
CREATE TABLE IF NOT EXISTS loan_type (
    id                       UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                     VARCHAR(80)   NOT NULL UNIQUE,
    minimum_amount           NUMERIC(12,2) NOT NULL CHECK (minimum_amount >= 0),
    maximum_amount           NUMERIC(12,2) NOT NULL CHECK (maximum_amount >= minimum_amount),
    annual_interest_percent  NUMERIC(5,2)  NOT NULL CHECK (annual_interest_percent >= 0 AND annual_interest_percent <= 100),
    automatic_validation     BOOLEAN       NOT NULL DEFAULT FALSE
    );

-- =========================
-- loan (LoanEntity)
-- =========================
CREATE TABLE IF NOT EXISTS loan (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    amount         NUMERIC(12,2) NOT NULL CHECK (amount >= 0),
    term_months    INTEGER      NOT NULL CHECK (term_months >= 1),
    email          VARCHAR(254) NOT NULL,
    id_state_loan  UUID         NOT NULL,
    id_type_loan   UUID         NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT fk_loan_state
    FOREIGN KEY (id_state_loan) REFERENCES loan_state(id)
    ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_loan_type
    FOREIGN KEY (id_type_loan) REFERENCES loan_type(id)
    ON UPDATE CASCADE ON DELETE RESTRICT
    );

-- Índices útiles
CREATE INDEX IF NOT EXISTS idx_loan_email       ON loan(email);
CREATE INDEX IF NOT EXISTS idx_loan_state       ON loan(id_state_loan);
CREATE INDEX IF NOT EXISTS idx_loan_type        ON loan(id_type_loan);

-- Seed: estado inicial
INSERT INTO loan_state (name, description)
VALUES ('PENDING_REVIEW', 'Pendiente de revisión')
    ON CONFLICT (name) DO NOTHING;