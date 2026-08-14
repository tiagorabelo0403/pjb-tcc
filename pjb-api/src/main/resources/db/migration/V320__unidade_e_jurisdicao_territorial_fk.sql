CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE tb_unidade_judiciaria_competencia ADD COLUMN tribunal_id BIGINT REFERENCES tb_tribunal(id);
ALTER TABLE tb_unidade_judiciaria_competencia ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

INSERT INTO tb_tribunal (sigla, nome, tipo_justica, grau, uf_sede)
SELECT
    codigos.tribunal_codigo,
    codigos.tribunal_codigo,
    codigos.tipo_justica,
    CASE codigos.tribunal_codigo
        WHEN 'STF' THEN 'CONSTITUCIONAL'
        WHEN 'STJ' THEN 'SUPERIOR'
        WHEN 'TST' THEN 'SUPERIOR'
        ELSE 'SEGUNDO_GRAU'
    END,
    codigos.uf
FROM (
    SELECT DISTINCT ON (u.tribunal_codigo)
        u.tribunal_codigo,
        u.tipo_justica,
        u.uf
    FROM tb_unidade_judiciaria_competencia u
    WHERE u.tribunal_codigo IS NOT NULL
      AND NOT EXISTS (SELECT 1 FROM tb_tribunal t WHERE t.sigla = u.tribunal_codigo)
    ORDER BY u.tribunal_codigo, u.id
) codigos;

UPDATE tb_unidade_judiciaria_competencia u
SET tribunal_id = t.id
FROM tb_tribunal t
WHERE t.sigla = u.tribunal_codigo;

UPDATE tb_unidade_judiciaria_competencia u
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = u.uf AND upper(unaccent(c.nome)) = upper(unaccent(u.comarca));

ALTER TABLE tb_unidade_judiciaria_competencia ALTER COLUMN tribunal_id SET NOT NULL;
ALTER TABLE tb_unidade_judiciaria_competencia DROP COLUMN tribunal_codigo;
ALTER TABLE tb_unidade_judiciaria_competencia DROP COLUMN comarca;

CREATE INDEX idx_unidade_competencia_tribunal ON tb_unidade_judiciaria_competencia (tribunal_id);
CREATE INDEX idx_unidade_competencia_territorio ON tb_unidade_judiciaria_competencia (uf, comarca_id);

ALTER TABLE tb_jurisdicao_territorial ADD COLUMN tribunal_id BIGINT REFERENCES tb_tribunal(id);
UPDATE tb_jurisdicao_territorial j SET tribunal_id = t.id FROM tb_tribunal t WHERE t.sigla = j.tribunal_codigo;
ALTER TABLE tb_jurisdicao_territorial ALTER COLUMN tribunal_id SET NOT NULL;
ALTER TABLE tb_jurisdicao_territorial DROP COLUMN tribunal_codigo;

ALTER TABLE tb_comarca ADD CONSTRAINT uk_comarca_municipio_tribunal UNIQUE (municipio_sede_ibge, tribunal_id);
