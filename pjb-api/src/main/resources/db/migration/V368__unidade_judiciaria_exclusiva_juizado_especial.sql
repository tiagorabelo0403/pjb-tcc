ALTER TABLE tb_unidade_judiciaria_competencia
    ADD COLUMN IF NOT EXISTS exclusivo_juizado_especial BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE tb_unidade_judiciaria_competencia
   SET exclusivo_juizado_especial = TRUE,
       permite_juizado_especial = TRUE,
       valor_causa_maximo = NULL
 WHERE codigo LIKE '%-JEC-%'
    OR tipo_vara = 'CRIMINAL_JUIZADO';

ALTER TABLE tb_unidade_judiciaria_competencia
    ADD CONSTRAINT ck_unidade_exclusiva_juizado_permite_juizado
    CHECK (NOT exclusivo_juizado_especial OR permite_juizado_especial);
