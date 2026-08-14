CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE jurisdicoes ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

UPDATE jurisdicoes j
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = j.estado AND upper(unaccent(c.nome)) = upper(unaccent(coalesce(j.comarca, j.municipio_sede)));

ALTER TABLE jurisdicoes DROP COLUMN comarca;
ALTER TABLE jurisdicoes DROP COLUMN municipio_sede;
ALTER TABLE jurisdicoes DROP COLUMN foro;
ALTER TABLE jurisdicoes DROP COLUMN estado;

CREATE INDEX idx_jurisdicao_comarca ON jurisdicoes (comarca_id);
