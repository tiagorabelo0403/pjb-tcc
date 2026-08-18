CREATE OR REPLACE FUNCTION pjb_sigilo_clearance_rank(raw_value TEXT)
RETURNS INTEGER
LANGUAGE SQL
IMMUTABLE
AS $$
    SELECT CASE UPPER(COALESCE(raw_value, 'PUBLICO'))
        WHEN 'SEGREDO_ESTADO' THEN 5
        WHEN 'SIGILO_N4' THEN 4
        WHEN 'SIGILO_N3' THEN 3
        WHEN 'SIGILO_N2' THEN 2
        WHEN 'SEGREDO_JUSTICA' THEN 1
        ELSE 0
    END
$$;

ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS tribunal VARCHAR(120);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS uf VARCHAR(2);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS unidade_judiciaria_codigo VARCHAR(80);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS data_atualizacao TIMESTAMP;

CREATE OR REPLACE VIEW vw_pjb_processo_sigilo_secure AS
SELECT
    p.id,
    p.numero_unificado,
    p.numero_processo,
    p.tribunal,
    p.uf,
    p.unidade_judiciaria_codigo,
    p.nivel_sigilo,
    p.status_processo,
    p.ramo_direito,
    p.fase_atual,
    p.data_criacao,
    p.data_atualizacao,
    p.data_ultima_movimentacao
FROM tb_processo p
WHERE
    COALESCE(p.nivel_sigilo, 'PUBLICO') = 'PUBLICO'
    OR (
        COALESCE(current_setting('app.pjb_sigilo_scope', true), '') <> ''
        AND pjb_sigilo_clearance_rank(current_setting('app.pjb_sigilo_clearance', true))
            >= pjb_sigilo_clearance_rank(COALESCE(p.nivel_sigilo, 'PUBLICO'))
        AND (
            p.unidade_judiciaria_codigo IS NULL
            OR COALESCE(current_setting('app.pjb_unit_code', true), '') = ''
            OR p.unidade_judiciaria_codigo = current_setting('app.pjb_unit_code', true)
        )
        AND (
            p.tribunal IS NULL
            OR COALESCE(current_setting('app.pjb_tribunal_code', true), '') = ''
            OR UPPER(p.tribunal) = UPPER(current_setting('app.pjb_tribunal_code', true))
        )
    );

COMMENT ON VIEW vw_pjb_processo_sigilo_secure IS
'Read model seguro para acesso a processos sigilosos com materialização de escopo, tribunal e clearance via current_setting(app.pjb_*).';
