-- V3__seed_loan_types.sql
-- Crea/actualiza dos tipos de préstamo: uno auto-validado y otro manual.

-- Tipo 1: PERSONAL_EXPRESS (validación automática)
INSERT INTO loan_type (
    name, minimum_amount, maximum_amount, annual_interest_percent, automatic_validation
) VALUES (
             'PERSONAL_EXPRESS', 300000.00, 5000000.00, 24.00, TRUE
         )
    ON CONFLICT (name) DO UPDATE
                              SET minimum_amount = EXCLUDED.minimum_amount,
                              maximum_amount = EXCLUDED.maximum_amount,
                              annual_interest_percent = EXCLUDED.annual_interest_percent,
                              automatic_validation = EXCLUDED.automatic_validation;

-- Tipo 2: PERSONAL_STANDARD (validación manual)
INSERT INTO loan_type (
    name, minimum_amount, maximum_amount, annual_interest_percent, automatic_validation
) VALUES (
             'PERSONAL_STANDARD', 300000.00, 20000000.00, 22.00, FALSE
         )
    ON CONFLICT (name) DO UPDATE
                              SET minimum_amount = EXCLUDED.minimum_amount,
                              maximum_amount = EXCLUDED.maximum_amount,
                              annual_interest_percent = EXCLUDED.annual_interest_percent,
                              automatic_validation = EXCLUDED.automatic_validation;