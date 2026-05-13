ALTER TABLE adv_office_policy
    ADD COLUMN IF NOT EXISTS force_patrono_certificate BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS allowed_ramos VARCHAR(1200) NOT NULL DEFAULT '';

ALTER TABLE adv_office_delegacao_regra
    ADD COLUMN IF NOT EXISTS allowed_ramos_override VARCHAR(1200) NOT NULL DEFAULT '';
