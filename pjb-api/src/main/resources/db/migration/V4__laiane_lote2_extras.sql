-- LAIANE Lote 2 Extras (PostgreSQL)
--
-- Este script cria tabelas complementares para suportar funcionalidades
-- avançadas do Lote 2, tais como agenda institucional, ofícios com
-- rastreamento, delegação de prazos e consolidação de lotes de processos.

-- =============================================================================
-- Agenda processual (evento_processual)
--
-- Armazena eventos processuais com início e fim no tempo, associando
-- processos e usuários responsáveis. Inclui soft delete e controle de
-- versão para concorrência. Índices aceleram consultas por processo,
-- responsável/status e faixa de datas.
-- =============================================================================
CREATE TABLE IF NOT EXISTS evento_processual (
    id            BIGSERIAL PRIMARY KEY,
    processo_id   BIGINT NOT NULL,
    responsavel_id BIGINT NOT NULL,
    tipo          VARCHAR(30) NOT NULL,
    status        VARCHAR(20) NOT NULL,
    titulo        VARCHAR(200) NOT NULL,
    descricao     TEXT NOT NULL,
    data_inicio   TIMESTAMP NOT NULL,
    data_fim      TIMESTAMP NOT NULL,
    criado_em     TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP NOT NULL DEFAULT NOW(),
    versao        BIGINT NOT NULL DEFAULT 0,
    ativo         BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_evento_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id),
    CONSTRAINT fk_evento_responsavel FOREIGN KEY (responsavel_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_evento_processo ON evento_processual(processo_id);
CREATE INDEX IF NOT EXISTS idx_evento_responsavel_status ON evento_processual(responsavel_id, status);
CREATE INDEX IF NOT EXISTS idx_evento_datas ON evento_processual(data_inicio, data_fim);

-- =============================================================================
-- Ofícios com rastreamento (tb_laiane_oficio)
--
-- Permite gerar, enviar e rastrear ofícios ou comunicados oficiais. O
-- tracking_code é um UUID que identifica de forma única cada ofício.
-- =============================================================================
CREATE TABLE IF NOT EXISTS tb_laiane_oficio (
    id              BIGSERIAL PRIMARY KEY,
    tracking_code   UUID NOT NULL UNIQUE,
    origem_id       BIGINT NOT NULL,
    destino_id      BIGINT,
    tipo            VARCHAR(40) NOT NULL,
    status          VARCHAR(40) NOT NULL,
    protocolo       VARCHAR(64),
    assunto         VARCHAR(200),
    conteudo        TEXT,
    enviado_em      TIMESTAMP,
    entregue_em     TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_oficio_origem FOREIGN KEY (origem_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_oficio_destino FOREIGN KEY (destino_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_laiane_oficio_origem ON tb_laiane_oficio(origem_id);
CREATE INDEX IF NOT EXISTS idx_laiane_oficio_status ON tb_laiane_oficio(status);

-- =============================================================================
-- Delegação de prazos (tb_laiane_deadline_delegation)
--
-- Permite que um usuário delegue a gestão de um prazo a outro usuário,
-- preservando o vínculo com o item de trabalho original (work_item). O
-- status indica se a delegação está pendente, aceita, concluída ou
-- cancelada.
-- =============================================================================
CREATE TABLE IF NOT EXISTS tb_laiane_deadline_delegation (
    id              BIGSERIAL PRIMARY KEY,
    work_item_id    BIGINT,
    delegator_id    BIGINT NOT NULL,
    delegatee_id    BIGINT NOT NULL,
    status          VARCHAR(20) NOT NULL,
    descricao       TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    accepted_at     TIMESTAMP,
    completed_at    TIMESTAMP,
    cancelled_at    TIMESTAMP,
    CONSTRAINT fk_delegation_work_item FOREIGN KEY (work_item_id) REFERENCES tb_work_item(id),
    CONSTRAINT fk_delegation_delegator FOREIGN KEY (delegator_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_delegation_delegatee FOREIGN KEY (delegatee_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_laiane_delegation_delegatee ON tb_laiane_deadline_delegation(delegatee_id);
CREATE INDEX IF NOT EXISTS idx_laiane_delegation_status ON tb_laiane_deadline_delegation(status);

-- =============================================================================
-- Consolidação de lotes de processos (tb_laiane_case_bundle)
--
-- Armazena listas de processos associados a uma tese ou objetivo comum,
-- permitindo tratar lote de processos de forma consolidada. O campo
-- processos_json contém uma lista de IDs de processo em formato JSON.
-- =============================================================================
CREATE TABLE IF NOT EXISTS tb_laiane_case_bundle (
    id              BIGSERIAL PRIMARY KEY,
    advogado_id     BIGINT NOT NULL,
    tese_id         BIGINT,
    descricao       TEXT,
    processos_json  TEXT NOT NULL,
    status          VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_case_bundle_advogado FOREIGN KEY (advogado_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_case_bundle_tese FOREIGN KEY (tese_id) REFERENCES tb_laiane_tese(id)
);

CREATE INDEX IF NOT EXISTS idx_laiane_case_bundle_advogado ON tb_laiane_case_bundle(advogado_id);
CREATE INDEX IF NOT EXISTS idx_laiane_case_bundle_status ON tb_laiane_case_bundle(status);