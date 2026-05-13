-- PJB V21: Categoria doc-level para impedir vazamento de documentos pessoais em processos públicos.
-- Postgres

ALTER TABLE tb_documento_processual
    ADD COLUMN IF NOT EXISTS categoria VARCHAR(20);

UPDATE tb_documento_processual
   SET categoria = 'PUBLICO'
 WHERE categoria IS NULL;

ALTER TABLE tb_documento_processual
    ALTER COLUMN categoria SET NOT NULL;

-- Índices auxiliares para filtros em consulta pública.
CREATE INDEX IF NOT EXISTS idx_doc_proc_sigilo ON tb_documento_processual (processo_id, nivel_sigilo);
CREATE INDEX IF NOT EXISTS idx_doc_proc_categoria ON tb_documento_processual (processo_id, categoria);
