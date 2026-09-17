CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE tb_escritura_extrajudicial_registro ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

UPDATE tb_escritura_extrajudicial_registro e
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = e.uf AND upper(unaccent(c.nome)) = upper(unaccent(e.comarca));

CREATE INDEX idx_escritura_extrajudicial_registro_comarca_fk ON tb_escritura_extrajudicial_registro (comarca_id);
