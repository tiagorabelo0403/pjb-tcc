-- V237: documento operational state and extended custody fields (Round 28AH)

ALTER TABLE pjb_documento_processual
    ADD COLUMN IF NOT EXISTS estado_operacional    VARCHAR(40),
    ADD COLUMN IF NOT EXISTS cadeia_custodia_id    UUID,
    ADD COLUMN IF NOT EXISTS protocolo_externo     VARCHAR(120),
    ADD COLUMN IF NOT EXISTS data_primeira_leitura TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS quantidade_paginas    INTEGER,
    ADD COLUMN IF NOT EXISTS lido_por_json         TEXT;

CREATE INDEX IF NOT EXISTS idx_doc_estado_operacional ON pjb_documento_processual (estado_operacional) WHERE estado_operacional IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_doc_cadeia_custodia    ON pjb_documento_processual (cadeia_custodia_id) WHERE cadeia_custodia_id IS NOT NULL;
