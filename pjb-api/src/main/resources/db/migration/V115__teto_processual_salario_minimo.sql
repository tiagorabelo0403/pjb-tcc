CREATE TABLE IF NOT EXISTS salario_minimo_nacional (
    id BIGSERIAL PRIMARY KEY,
    ano_referencia INTEGER NOT NULL,
    valor_mensal NUMERIC(19,2) NOT NULL,
    valor_diario NUMERIC(19,2) NOT NULL,
    valor_hora NUMERIC(19,2) NOT NULL,
    vigente_desde DATE NOT NULL,
    vigente_ate DATE,
    norma_referencia VARCHAR(200) NOT NULL,
    fonte_oficial VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    versao BIGINT
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_salario_minimo_ano ON salario_minimo_nacional (ano_referencia);
CREATE INDEX IF NOT EXISTS idx_salario_minimo_vigencia ON salario_minimo_nacional (vigente_desde, vigente_ate);

INSERT INTO salario_minimo_nacional (
    ano_referencia,
    valor_mensal,
    valor_diario,
    valor_hora,
    vigente_desde,
    vigente_ate,
    norma_referencia,
    fonte_oficial,
    ativo,
    atualizado_em,
    versao
)
SELECT 2024, 1412.00, 47.07, 6.42, DATE '2024-01-01', NULL, 'Decreto 11.864/2023', 'Planalto', TRUE, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM salario_minimo_nacional WHERE ano_referencia = 2024);

INSERT INTO salario_minimo_nacional (
    ano_referencia,
    valor_mensal,
    valor_diario,
    valor_hora,
    vigente_desde,
    vigente_ate,
    norma_referencia,
    fonte_oficial,
    ativo,
    atualizado_em,
    versao
)
SELECT 2025, 1518.00, 50.60, 6.90, DATE '2025-01-01', NULL, 'Decreto 12.342/2024', 'Planalto', TRUE, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM salario_minimo_nacional WHERE ano_referencia = 2025);

INSERT INTO salario_minimo_nacional (
    ano_referencia,
    valor_mensal,
    valor_diario,
    valor_hora,
    vigente_desde,
    vigente_ate,
    norma_referencia,
    fonte_oficial,
    ativo,
    atualizado_em,
    versao
)
SELECT 2026, 1621.00, 54.04, 7.37, DATE '2026-01-01', NULL, 'Decreto 12.797/2025', 'Planalto', TRUE, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM salario_minimo_nacional WHERE ano_referencia = 2026);
