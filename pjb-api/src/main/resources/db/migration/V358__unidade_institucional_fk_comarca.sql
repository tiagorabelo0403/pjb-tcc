CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE tb_unidade_institucional ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

UPDATE tb_unidade_institucional u
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = u.uf AND upper(unaccent(c.nome)) = upper(unaccent(u.comarca));

CREATE INDEX idx_unidade_institucional_comarca_fk ON tb_unidade_institucional (comarca_id);
