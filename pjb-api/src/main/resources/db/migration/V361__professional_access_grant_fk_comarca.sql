CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE tb_professional_access_grant ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

UPDATE tb_professional_access_grant p
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = p.uf AND upper(unaccent(c.nome)) = upper(unaccent(p.comarca));

CREATE INDEX idx_professional_access_grant_comarca_fk ON tb_professional_access_grant (comarca_id);
