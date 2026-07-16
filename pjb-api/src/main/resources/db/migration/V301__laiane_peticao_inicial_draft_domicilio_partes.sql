-- Aditiva: não migra dado histórico. Sessões de draft já existentes ficam com os 5 campos
-- nulos/default, comportamento idêntico ao de hoje. Sem backfill necessário.
ALTER TABLE tb_laiane_peticao_inicial_draft
    ADD COLUMN IF NOT EXISTS uf_autor VARCHAR(2),
    ADD COLUMN IF NOT EXISTS comarca_autor VARCHAR(120),
    ADD COLUMN IF NOT EXISTS uf_reu VARCHAR(2),
    ADD COLUMN IF NOT EXISTS comarca_reu VARCHAR(120),
    ADD COLUMN IF NOT EXISTS endereco_reu_desconhecido BOOLEAN NOT NULL DEFAULT FALSE;
