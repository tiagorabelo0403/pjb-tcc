CREATE EXTENSION IF NOT EXISTS unaccent;

ALTER TABLE tb_usuario ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

UPDATE tb_usuario u
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = u.uf AND upper(unaccent(c.nome)) = upper(unaccent(u.comarca));

ALTER TABLE tb_processo ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);
ALTER TABLE tb_processo ADD COLUMN comarca_autor_id BIGINT REFERENCES tb_comarca(id);
ALTER TABLE tb_processo ADD COLUMN comarca_reu_id BIGINT REFERENCES tb_comarca(id);

UPDATE tb_processo p
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = p.uf AND upper(unaccent(c.nome)) = upper(unaccent(p.comarca));

UPDATE tb_processo p
SET comarca_autor_id = c.id
FROM tb_comarca c
WHERE c.uf = p.uf_autor AND upper(unaccent(c.nome)) = upper(unaccent(p.comarca_autor));

UPDATE tb_processo p
SET comarca_reu_id = c.id
FROM tb_comarca c
WHERE c.uf = p.uf_reu AND upper(unaccent(c.nome)) = upper(unaccent(p.comarca_reu));

ALTER TABLE tb_work_item ADD COLUMN comarca_id BIGINT REFERENCES tb_comarca(id);

UPDATE tb_work_item w
SET comarca_id = c.id
FROM tb_comarca c
WHERE c.uf = w.uf AND upper(unaccent(c.nome)) = upper(unaccent(w.comarca));

CREATE INDEX idx_usuario_comarca ON tb_usuario (comarca_id);
CREATE INDEX idx_processo_comarca ON tb_processo (comarca_id);
CREATE INDEX idx_processo_comarca_autor ON tb_processo (comarca_autor_id);
CREATE INDEX idx_processo_comarca_reu ON tb_processo (comarca_reu_id);
CREATE INDEX idx_workitem_comarca ON tb_work_item (comarca_id);
