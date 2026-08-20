CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE orgaos_judiciarios ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

UPDATE orgaos_judiciarios o
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = o.estado AND upper(unaccent(c.nome)) = upper(unaccent(o.comarca));

CREATE INDEX idx_orgao_comarca_fk ON orgaos_judiciarios (comarca_id);
