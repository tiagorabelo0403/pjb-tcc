CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE jurisdicoes ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

UPDATE jurisdicoes j
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = j.estado AND upper(unaccent(c.nome)) = upper(unaccent(coalesce(j.comarca, j.municipio_sede)));

DO $$
DECLARE
    pendentes INT;
BEGIN
    SELECT count(*) INTO pendentes
    FROM jurisdicoes
    WHERE municipio_sede IS NOT NULL OR foro IS NOT NULL;
    IF pendentes > 0 THEN
        RAISE EXCEPTION 'jurisdicoes.municipio_sede/foro possuem % linha(s) com dado real, mas esta migration nao mantem fallback pra esses 2 campos (apenas comarca/estado sao preservados). Investigar a origem desse dado antes de prosseguir — nao ha caminho de escrita conhecido via API pra esses campos.', pendentes;
    END IF;
END $$;

ALTER TABLE jurisdicoes DROP COLUMN municipio_sede;
ALTER TABLE jurisdicoes DROP COLUMN foro;

CREATE INDEX idx_jurisdicao_comarca ON jurisdicoes (comarca_id);
