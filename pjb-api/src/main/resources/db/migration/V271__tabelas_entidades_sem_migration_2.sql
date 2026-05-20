-- tb_audit_correlation_index
CREATE TABLE IF NOT EXISTS tb_audit_correlation_index (
    id              BIGSERIAL       PRIMARY KEY,
    correlation_key VARCHAR(180)    NOT NULL,
    resource_type   VARCHAR(64)     NOT NULL,
    resource_id     VARCHAR(120)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL,
    ver             BIGINT          NOT NULL DEFAULT 0
);

-- tb_auditoria_acesso
CREATE TABLE IF NOT EXISTS tb_auditoria_acesso (
    id               BIGSERIAL    PRIMARY KEY,
    usuario          VARCHAR(255),
    acao             VARCHAR(255),
    numero_processo  VARCHAR(255),
    data_hora        TIMESTAMP
);

-- tb_chat_mensagem
CREATE TABLE IF NOT EXISTS tb_chat_mensagem (
    id          BIGSERIAL    PRIMARY KEY,
    conteudo    TEXT         NOT NULL,
    data_envio  TIMESTAMP    NOT NULL,
    usuario_id  BIGINT       NOT NULL,
    processo_id BIGINT       NOT NULL,
    CONSTRAINT fk_chat_usuario  FOREIGN KEY (usuario_id)  REFERENCES tb_usuario(id),
    CONSTRAINT fk_chat_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id)
);

CREATE INDEX IF NOT EXISTS idx_chat_usuario  ON tb_chat_mensagem (usuario_id);
CREATE INDEX IF NOT EXISTS idx_chat_processo ON tb_chat_mensagem (processo_id);

-- tb_credencial_acesso
CREATE TABLE IF NOT EXISTS tb_credencial_acesso (
    id          UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    login       VARCHAR(255) NOT NULL UNIQUE,
    senha_hash  VARCHAR(255) NOT NULL,
    validade    TIMESTAMP,
    ativa       BOOLEAN,
    processo_id BIGINT       NOT NULL,
    CONSTRAINT fk_credencial_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id)
);

-- tb_cross_audit_link
CREATE TABLE IF NOT EXISTS tb_cross_audit_link (
    id                  BIGSERIAL    PRIMARY KEY,
    correlation_key     VARCHAR(180) NOT NULL,
    left_resource_type  VARCHAR(64)  NOT NULL,
    left_resource_id    VARCHAR(120) NOT NULL,
    right_resource_type VARCHAR(64)  NOT NULL,
    right_resource_id   VARCHAR(120) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL,
    ver                 BIGINT       NOT NULL DEFAULT 0
);

-- tb_execution_mesh_state
CREATE TABLE IF NOT EXISTS tb_execution_mesh_state (
    aggregate_id                    VARCHAR(80)    NOT NULL PRIMARY KEY,
    processo_id                     BIGINT,
    numero_processo                 VARCHAR(50),
    species_code                    VARCHAR(80)    NOT NULL,
    current_stage                   VARCHAR(80)    NOT NULL,
    current_queue                   VARCHAR(120),
    current_inbox                   VARCHAR(120),
    current_impact                  VARCHAR(120),
    current_asset_kind              VARCHAR(80),
    current_gateway                 VARCHAR(120),
    external_status                 VARCHAR(80),
    terminal_disposition            VARCHAR(120),
    current_expropriation_mode      VARCHAR(120),
    current_auction_cycle_mode      VARCHAR(140),
    current_contingency_mode        VARCHAR(140),
    current_homologation_mode       VARCHAR(160),
    current_closure_mode            VARCHAR(160),
    closure_consistency_status      VARCHAR(160),
    current_preference_mode         VARCHAR(160),
    subrogation_status              VARCHAR(160),
    reconciliation_status           VARCHAR(120),
    archive_link_status             VARCHAR(120),
    satisfaction_state              VARCHAR(120),
    satisfaction_percent            NUMERIC(7, 4),
    residual_amount                 NUMERIC(19, 2),
    incident_count                  INT            NOT NULL DEFAULT 0,
    enforcement_count               INT            NOT NULL DEFAULT 0,
    snapshot_json                   TEXT           NOT NULL,
    incident_ledger_json            TEXT           NOT NULL,
    enforcement_ledger_json         TEXT           NOT NULL,
    patrimonial_ledger_json         TEXT           NOT NULL,
    external_ledger_json            TEXT           NOT NULL,
    terminal_ledger_json            TEXT           NOT NULL,
    expropriation_ledger_json       TEXT           NOT NULL,
    auction_cycle_ledger_json       TEXT           NOT NULL,
    contingency_ledger_json         TEXT           NOT NULL,
    reconciliation_ledger_json      TEXT           NOT NULL,
    archive_ledger_json             TEXT           NOT NULL,
    homologation_ledger_json        TEXT           NOT NULL,
    settlement_ledger_json          TEXT           NOT NULL,
    closure_ledger_json             TEXT           NOT NULL,
    integrity_fingerprint           VARCHAR(64)    NOT NULL,
    row_version                     BIGINT         NOT NULL DEFAULT 0,
    created_at                      TIMESTAMPTZ    NOT NULL,
    updated_at                      TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_mesh_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id)
);

-- tb_fonte_soberana_snapshot
CREATE TABLE IF NOT EXISTS tb_fonte_soberana_snapshot (
    id                  BIGSERIAL    PRIMARY KEY,
    processo_id         BIGINT       NOT NULL,
    numero_processo     VARCHAR(80)  NOT NULL,
    confiabilidade_media INT         NOT NULL,
    possui_conflito     BOOLEAN      NOT NULL,
    exige_refresh       BOOLEAN      NOT NULL,
    digest              VARCHAR(128) NOT NULL,
    payload_json        TEXT         NOT NULL,
    atualizado_em       TIMESTAMPTZ  NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_fonte_soberana_processo ON tb_fonte_soberana_snapshot (processo_id);
CREATE        INDEX IF NOT EXISTS idx_fonte_soberana_digest   ON tb_fonte_soberana_snapshot (digest);

-- tb_identidade_grafo_projection_batch
CREATE TABLE IF NOT EXISTS tb_identidade_grafo_projection_batch (
    id                BIGSERIAL    PRIMARY KEY,
    correlacao_id     VARCHAR(160) NOT NULL,
    backend           VARCHAR(80)  NOT NULL,
    storage_key       VARCHAR(400) NOT NULL,
    merge_fingerprint VARCHAR(128) NOT NULL,
    total_vertices    INT          NOT NULL,
    total_arestas     INT          NOT NULL,
    projection_json   TEXT         NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_identidade_batch_correlacao   ON tb_identidade_grafo_projection_batch (correlacao_id, created_at);
CREATE INDEX IF NOT EXISTS idx_identidade_batch_fingerprint  ON tb_identidade_grafo_projection_batch (merge_fingerprint);

-- tb_institutional_policy_profile
CREATE TABLE IF NOT EXISTS tb_institutional_policy_profile (
    id                    BIGSERIAL    PRIMARY KEY,
    processo_id           BIGINT,
    equipe_id             BIGINT,
    policy_key            VARCHAR(160) NOT NULL,
    policy_tier           VARCHAR(80)  NOT NULL,
    policy_version        VARCHAR(80)  NOT NULL,
    ramo_direito          VARCHAR(80),
    materia               VARCHAR(80),
    rito_processual       VARCHAR(120),
    classe_processual     VARCHAR(160),
    fase_processual       VARCHAR(80),
    tribunal_codigo       VARCHAR(80),
    approval_required     BOOLEAN      NOT NULL,
    strict_release        BOOLEAN      NOT NULL,
    mandatory_directives  TEXT,
    blocking_directives   TEXT,
    release_guardrails    TEXT,
    escalation_triggers   TEXT,
    data_criacao          TIMESTAMP    NOT NULL,
    data_atualizacao      TIMESTAMP    NOT NULL
);

-- tb_kernel_decision_event
CREATE TABLE IF NOT EXISTS tb_kernel_decision_event (
    id                      BIGSERIAL   PRIMARY KEY,
    processo_id             BIGINT      NOT NULL,
    proposta_id             BIGINT,
    usuario_id              BIGINT,
    scope                   VARCHAR(80) NOT NULL,
    decision_code           VARCHAR(80) NOT NULL,
    release_allowed         BOOLEAN     NOT NULL,
    approval_required       BOOLEAN     NOT NULL,
    internal_draft_required BOOLEAN     NOT NULL,
    risk_level              VARCHAR(60),
    reasons                 TEXT,
    mandatory_actions       TEXT,
    diagnostics             TEXT,
    data_criacao            TIMESTAMP   NOT NULL
);

-- tb_negotiation_round_snapshot
CREATE TABLE IF NOT EXISTS tb_negotiation_round_snapshot (
    id                BIGSERIAL   PRIMARY KEY,
    processo_id       BIGINT      NOT NULL,
    proposta_id       BIGINT,
    usuario_id        BIGINT,
    round_scope       VARCHAR(80) NOT NULL,
    approval_band     VARCHAR(80),
    release_mode      VARCHAR(80),
    risk_level        VARCHAR(60),
    summary           TEXT,
    suggested_message TEXT,
    mandatory_actions TEXT,
    data_criacao      TIMESTAMP   NOT NULL
);

-- tb_process_intelligence_snapshot
CREATE TABLE IF NOT EXISTS tb_process_intelligence_snapshot (
    id              BIGSERIAL    PRIMARY KEY,
    processo_id     BIGINT       NOT NULL,
    snapshot_scope  VARCHAR(80)  NOT NULL,
    policy_key      VARCHAR(160),
    policy_tier     VARCHAR(80),
    risk_level      VARCHAR(60),
    decision_code   VARCHAR(80),
    strategic_focus TEXT,
    telemetry       TEXT,
    data_criacao    TIMESTAMP    NOT NULL
);

-- tb_request_idempotency
CREATE TABLE IF NOT EXISTS tb_request_idempotency (
    request_hash  VARCHAR(96)  NOT NULL PRIMARY KEY,
    status        VARCHAR(16)  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    lock_until    TIMESTAMPTZ,
    resource_type VARCHAR(64),
    resource_id   VARCHAR(120),
    response_hash VARCHAR(96),
    response_json TEXT,
    ver           BIGINT       NOT NULL DEFAULT 0
);

-- tb_usuario_calculo_experience_pref
CREATE TABLE IF NOT EXISTS tb_usuario_calculo_experience_pref (
    id                   BIGSERIAL    PRIMARY KEY,
    principal_key        VARCHAR(190) NOT NULL,
    equipe_ativa_id      BIGINT,
    domain_code          VARCHAR(80),
    experience_mode      VARCHAR(40)  NOT NULL,
    scope_type           VARCHAR(30)  NOT NULL,
    ramo_direito         VARCHAR(80),
    classe_processual    VARCHAR(120),
    tipo_causa           VARCHAR(120),
    perfil_equipe        VARCHAR(80),
    tribunal             VARCHAR(80),
    sistema_origem       VARCHAR(80),
    institutional_policy BOOLEAN      NOT NULL,
    updated_at           TIMESTAMPTZ  NOT NULL
);

-- orgaos_judiciarios
CREATE TABLE IF NOT EXISTS orgaos_judiciarios (
    id                BIGSERIAL    PRIMARY KEY,
    nome              VARCHAR(255) NOT NULL,
    sigla             VARCHAR(30)  NOT NULL,
    tipo              VARCHAR(50)  NOT NULL,
    esfera            VARCHAR(50)  NOT NULL,
    comarca           VARCHAR(100),
    estado            VARCHAR(2),
    adapter_bean_name VARCHAR(100),
    ativo             BOOLEAN      NOT NULL DEFAULT true,
    data_criacao      TIMESTAMP    NOT NULL,
    CONSTRAINT uk_orgao_sigla UNIQUE (sigla)
);

CREATE INDEX IF NOT EXISTS idx_orgao_nome    ON orgaos_judiciarios (nome);
CREATE INDEX IF NOT EXISTS idx_orgao_tipo    ON orgaos_judiciarios (tipo);
CREATE INDEX IF NOT EXISTS idx_orgao_comarca ON orgaos_judiciarios (comarca);

-- orgao_competencia_jurisdicao (join table for OrgaoJudiciario <-> Jurisdicao)
CREATE TABLE IF NOT EXISTS orgao_competencia_jurisdicao (
    orgao_id      BIGINT NOT NULL,
    jurisdicao_id BIGINT NOT NULL,
    PRIMARY KEY (orgao_id, jurisdicao_id),
    CONSTRAINT fk_ocj_orgao      FOREIGN KEY (orgao_id)      REFERENCES orgaos_judiciarios(id),
    CONSTRAINT fk_ocj_jurisdicao FOREIGN KEY (jurisdicao_id) REFERENCES jurisdicoes(id)
);
