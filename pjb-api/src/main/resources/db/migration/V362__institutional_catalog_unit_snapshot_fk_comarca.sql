CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE tb_inst_catalog_unit_snapshot ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

UPDATE tb_inst_catalog_unit_snapshot t
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = t.uf AND upper(unaccent(c.nome)) = upper(unaccent(t.comarca));

CREATE INDEX idx_inst_catalog_unit_snapshot_comarca_fk ON tb_inst_catalog_unit_snapshot (comarca_id);
