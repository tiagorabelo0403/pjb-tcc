-- Pasta Digital + PageID (PJB 2026)

CREATE TABLE IF NOT EXISTS tb_documento_processual (
    id UUID PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    nome_original VARCHAR(255),
    titulo VARCHAR(255),
    content_type VARCHAR(100),
    tamanho_bytes BIGINT,
    sha256 VARCHAR(64),
    pdf BYTEA,
    nivel_sigilo VARCHAR(40) DEFAULT 'PUBLICO',
    origem_sistema VARCHAR(80),
    criado_por BIGINT,
    criado_em TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tb_documento_pagina (
    id UUID PRIMARY KEY,
    documento_id UUID NOT NULL,
    page_number INT NOT NULL,
    page_id VARCHAR(80) NOT NULL UNIQUE,
    fingerprint VARCHAR(64) NOT NULL,
    texto_extraido TEXT,
    criado_em TIMESTAMP
);

-- Full-text index (Postgres) para busca rápida por página
DO $$ BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_name = 'tb_documento_pagina'
           AND column_name = 'texto_tsv'
    ) THEN
        ALTER TABLE tb_documento_pagina
            ADD COLUMN texto_tsv tsvector GENERATED ALWAYS AS (to_tsvector('portuguese', coalesce(texto_extraido,''))) STORED;
    END IF;
END $$;

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_doc_processual_processo') THEN
        ALTER TABLE tb_documento_processual
            ADD CONSTRAINT fk_doc_processual_processo
            FOREIGN KEY (processo_id) REFERENCES tb_processo(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_doc_pagina_documento') THEN
        ALTER TABLE tb_documento_pagina
            ADD CONSTRAINT fk_doc_pagina_documento
            FOREIGN KEY (documento_id) REFERENCES tb_documento_processual(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_doc_processual_processo_id ON tb_documento_processual(processo_id);
CREATE UNIQUE INDEX IF NOT EXISTS ux_doc_processual_processo_sha256 ON tb_documento_processual(processo_id, sha256);
CREATE INDEX IF NOT EXISTS idx_doc_pagina_documento_id ON tb_documento_pagina(documento_id);
CREATE INDEX IF NOT EXISTS idx_doc_pagina_page_id ON tb_documento_pagina(page_id);
CREATE INDEX IF NOT EXISTS idx_doc_pagina_tsv ON tb_documento_pagina USING GIN (texto_tsv);
