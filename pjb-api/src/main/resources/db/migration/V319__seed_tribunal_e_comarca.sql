WITH tribunal_list AS (
    SELECT DISTINCT
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
SELECT DISTINCT
    j.municipio_nome,
    j.uf,
    j.municipio_ibge,
    NULL,
    ti.id
FROM tb_jurisdicao_territorial j
JOIN tribunal_inserted ti ON j.tribunal_codigo = ti.sigla
WHERE j.municipio_ibge IS NOT NULL;
