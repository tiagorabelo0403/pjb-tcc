ALTER TABLE pjb_sisbajud_operacao
    ADD COLUMN IF NOT EXISTS cpf_devedor VARCHAR(14),
    ADD COLUMN IF NOT EXISTS proximo_retry_em TIMESTAMPTZ;

ALTER TABLE pjb_renajud_restricao
    ADD COLUMN IF NOT EXISTS tentativas INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS proximo_retry_em TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS confirmado_em TIMESTAMPTZ;

ALTER TABLE pjb_infojud_consulta
    ADD COLUMN IF NOT EXISTS tentativas INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS proximo_retry_em TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_sisbajud_retry ON pjb_sisbajud_operacao (status, proximo_retry_em);
CREATE INDEX IF NOT EXISTS idx_renajud_retry ON pjb_renajud_restricao (status, proximo_retry_em);
CREATE INDEX IF NOT EXISTS idx_infojud_retry ON pjb_infojud_consulta (status, proximo_retry_em);
