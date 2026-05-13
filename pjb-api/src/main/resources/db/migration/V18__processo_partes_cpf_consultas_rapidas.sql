-- V18: PROJUDI/PJe alignment
-- 1) Persistir CPF das partes (autor/réu) para buscas autenticadas (consulta rápida).
-- 2) Índices para desempenho.

ALTER TABLE tb_processo ADD COLUMN parte_autora_cpf VARCHAR(11);
ALTER TABLE tb_processo ADD COLUMN parte_reu_cpf VARCHAR(11);

CREATE INDEX idx_processo_parte_autora_cpf ON tb_processo (parte_autora_cpf);
CREATE INDEX idx_processo_parte_reu_cpf ON tb_processo (parte_reu_cpf);
CREATE INDEX idx_processo_parte_autora_nome ON tb_processo (parte_autora_nome);
CREATE INDEX idx_processo_parte_reu_nome ON tb_processo (parte_reu_nome);
