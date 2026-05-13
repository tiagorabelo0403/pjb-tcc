CREATE TABLE memory_candidate_review (
    id               UUID          NOT NULL,
    candidate_id     UUID          NOT NULL,
    session_id       VARCHAR(255)  NOT NULL,
    modulo_origem    VARCHAR(100)  NOT NULL,
    tipo             VARCHAR(50)   NOT NULL,
    chave            VARCHAR(500)  NOT NULL,
    conteudo         TEXT          NOT NULL,
    motivo_extracao  TEXT,
    sigilo_nivel     VARCHAR(50)   NOT NULL,
    status           VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    revisado_por     VARCHAR(255),
    motivo_rejeicao  TEXT,
    criado_em        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    revisado_em      TIMESTAMPTZ,
    CONSTRAINT pk_memory_candidate_review PRIMARY KEY (id)
);

ALTER TABLE memory_candidate_review ENABLE ROW LEVEL SECURITY;

CREATE INDEX idx_candidate_review_pendente
    ON memory_candidate_review (criado_em)
    WHERE status = 'PENDENTE';

CREATE INDEX idx_candidate_review_modulo
    ON memory_candidate_review (modulo_origem, status);

CREATE INDEX idx_candidate_review_candidate
    ON memory_candidate_review (candidate_id);
