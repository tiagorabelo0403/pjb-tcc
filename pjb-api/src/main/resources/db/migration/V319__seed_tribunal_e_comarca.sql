CREATE EXTENSION IF NOT EXISTS unaccent;

WITH tribunal_list AS (
    SELECT DISTINCT ON (j.tribunal_codigo)
        j.tribunal_codigo AS sigla,
        CASE j.tribunal_codigo
            WHEN 'TRT7' THEN 'Tribunal Regional do Trabalho da 7ª Região'
            WHEN 'TRT3' THEN 'Tribunal Regional do Trabalho da 3ª Região'
            WHEN 'TRT21' THEN 'Tribunal Regional do Trabalho da 21ª Região'
            ELSE j.tribunal_codigo
        END AS nome,
        j.tipo_justica::VARCHAR AS tipo_justica,
        'SEGUNDO_GRAU' AS grau,
        j.uf AS uf_sede
    FROM tb_jurisdicao_territorial j
    WHERE j.tribunal_codigo IS NOT NULL
    ORDER BY j.tribunal_codigo, j.id
),
tribunal_inserted AS (
    INSERT INTO tb_tribunal (sigla, nome, tipo_justica, grau, uf_sede)
    SELECT
        sigla,
        nome,
        tipo_justica,
        grau,
        uf_sede
    FROM tribunal_list
    RETURNING id, sigla
)
INSERT INTO tb_comarca (nome, uf, municipio_sede_ibge, nome_foro, tribunal_id)
SELECT DISTINCT ON (j.municipio_ibge)
    j.municipio_nome,
    j.uf,
    j.municipio_ibge,
    NULL,
    ti.id
FROM tb_jurisdicao_territorial j
JOIN tribunal_inserted ti ON j.tribunal_codigo = ti.sigla
WHERE j.municipio_ibge IS NOT NULL
ORDER BY j.municipio_ibge, j.id;

DO $$
DECLARE
    ambiguas INT;
BEGIN
    SELECT count(*) INTO ambiguas
    FROM (
        SELECT uf, upper(unaccent(nome))
        FROM tb_comarca
        GROUP BY 1, 2
        HAVING count(*) > 1
    ) duplicadas;
    IF ambiguas > 0 THEN
        RAISE EXCEPTION 'tb_comarca possui % par(es) (uf, nome normalizado) duplicado(s). Os backfills de V320-V322 resolvem comarca_id por (uf, upper(unaccent(nome))) e escolheriam uma linha nao determinada nesse caso. Corrigir o catalogo de origem (tb_jurisdicao_territorial) antes de prosseguir.', ambiguas;
    END IF;
END $$;
