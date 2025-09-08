INSERT INTO loan_state (name, description)
VALUES ('MANUAL_REVIEW', 'revisión manual')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO loan_state (name, description)
VALUES ('REJECTED', 'Rechazado')
    ON CONFLICT (name) DO NOTHING;

INSERT INTO loan_state (name, description)
VALUES ('APPROVED', 'Aprobado')
    ON CONFLICT (name) DO NOTHING;