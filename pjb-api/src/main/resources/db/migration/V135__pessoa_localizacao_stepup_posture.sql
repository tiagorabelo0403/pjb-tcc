ALTER TABLE tb_pessoa_localizacao_consulta
    ADD COLUMN IF NOT EXISTS step_up_required BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS step_up_satisfied BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS challenge_hint VARCHAR(180) NULL;

CREATE INDEX IF NOT EXISTS idx_pessoa_localizacao_stepup ON tb_pessoa_localizacao_consulta(step_up_required, step_up_satisfied, created_at DESC);
