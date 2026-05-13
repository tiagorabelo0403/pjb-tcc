-- Serviços Judiciais (PJB 2026) - Knowledge Cards

CREATE TABLE IF NOT EXISTS tb_knowledge_card (
    id BIGSERIAL PRIMARY KEY,
    dominio VARCHAR(40) NOT NULL,
    titulo VARCHAR(260) NOT NULL,
    conteudo TEXT NOT NULL,
    tags VARCHAR(600),
    fonte VARCHAR(120),
    criado_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_kcard_dominio ON tb_knowledge_card(dominio);
