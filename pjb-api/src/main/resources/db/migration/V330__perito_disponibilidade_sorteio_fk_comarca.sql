CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE tb_perito_disponibilidade ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);
ALTER TABLE tb_perito_sorteio_audit ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

UPDATE tb_perito_disponibilidade t
SET comarca_id = c.id
FROM tb_comarca c
WHERE upper(unaccent(c.nome)) = upper(unaccent(t.comarca))
  AND (SELECT count(*) FROM tb_comarca c2 WHERE upper(unaccent(c2.nome)) = upper(unaccent(t.comarca))) = 1;

UPDATE tb_perito_sorteio_audit t
SET comarca_id = c.id
FROM tb_comarca c
WHERE upper(unaccent(c.nome)) = upper(unaccent(t.comarca))
  AND (SELECT count(*) FROM tb_comarca c2 WHERE upper(unaccent(c2.nome)) = upper(unaccent(t.comarca))) = 1;

CREATE INDEX idx_perito_disponibilidade_comarca_fk ON tb_perito_disponibilidade (comarca_id);
CREATE INDEX idx_perito_sorteio_audit_comarca_fk ON tb_perito_sorteio_audit (comarca_id);
