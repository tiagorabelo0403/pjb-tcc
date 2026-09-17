CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE operational_function_credentials ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

UPDATE operational_function_credentials o
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = o.uf AND upper(unaccent(c.nome)) = upper(unaccent(o.comarca));

CREATE INDEX idx_operational_function_credentials_comarca_fk ON operational_function_credentials (comarca_id);
