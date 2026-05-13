CREATE TABLE pjb_digitalizacao_job (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT REFERENCES tb_processo(id) ON DELETE SET NULL,
 numero_processo_origem VARCHAR(64),
 sistema_origem VARCHAR(32),
 total_paginas INT,
 paginas_processadas INT NOT NULL DEFAULT 0,
 status VARCHAR(32) NOT NULL DEFAULT 'PENDENTE',
 ocr_engine VARCHAR(32) NOT NULL DEFAULT 'TESSERACT_5',
 idioma VARCHAR(8) NOT NULL DEFAULT 'por',
 confianca_media NUMERIC(5,2),
 revisao_requerida BOOLEAN NOT NULL DEFAULT FALSE,
 operador_id BIGINT REFERENCES tb_usuario(id),
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 started_at TIMESTAMPTZ,
 completed_at TIMESTAMPTZ,
 failure_reason TEXT
);
CREATE INDEX idx_digitalizacao_status ON pjb_digitalizacao_job (status, created_at);
CREATE TABLE pjb_digitalizacao_pagina (
 id BIGSERIAL PRIMARY KEY,
 job_id BIGINT NOT NULL REFERENCES pjb_digitalizacao_job(id) ON DELETE CASCADE,
 numero_pagina INT NOT NULL,
 conteudo_ocr TEXT,
 confianca NUMERIC(5,2),
 tipo_peca VARCHAR(64),
 revisado BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 CONSTRAINT uk_digitalizacao_pagina UNIQUE (job_id, numero_pagina)
);
CREATE INDEX idx_digitalizacao_pagina_job ON pjb_digitalizacao_pagina (job_id);
CREATE INDEX idx_digitalizacao_pagina_tipo ON pjb_digitalizacao_pagina (tipo_peca);
