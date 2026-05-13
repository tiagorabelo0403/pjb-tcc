-- Kernel Recursal: CaseFile + Grafo de Proceedings (recurso/incident/remessa)

CREATE TABLE IF NOT EXISTS tb_case_file (
    id BIGSERIAL PRIMARY KEY,
    root_processo_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_case_file_root_processo UNIQUE (root_processo_id)
);

CREATE TABLE IF NOT EXISTS tb_case_proceeding (
    id BIGSERIAL PRIMARY KEY,
    case_file_id BIGINT NOT NULL,
    proceeding_key VARCHAR(64) NOT NULL,
    shadow BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    instance_level VARCHAR(30) NOT NULL,
    court VARCHAR(120),
    numero_unificado VARCHAR(50),
    linked_processo_id BIGINT,
    secrecy VARCHAR(40) NOT NULL,
    source_system VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_case_proceeding_key UNIQUE (proceeding_key)
);

CREATE INDEX IF NOT EXISTS ix_case_proceeding_case_file ON tb_case_proceeding(case_file_id);
CREATE INDEX IF NOT EXISTS ix_case_proceeding_numero ON tb_case_proceeding(numero_unificado);

CREATE TABLE IF NOT EXISTS tb_case_edge (
    id BIGSERIAL PRIMARY KEY,
    case_file_id BIGINT NOT NULL,
    from_proceeding_key VARCHAR(64) NOT NULL,
    to_proceeding_key VARCHAR(64) NOT NULL,
    relation_type VARCHAR(40) NOT NULL,
    appeal_type VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_case_edge UNIQUE (case_file_id, from_proceeding_key, to_proceeding_key, relation_type, appeal_type)
);

CREATE INDEX IF NOT EXISTS ix_case_edge_case_file ON tb_case_edge(case_file_id);
CREATE INDEX IF NOT EXISTS ix_case_edge_from ON tb_case_edge(from_proceeding_key);
CREATE INDEX IF NOT EXISTS ix_case_edge_to ON tb_case_edge(to_proceeding_key);

CREATE TABLE IF NOT EXISTS tb_case_file_event (
    id BIGSERIAL PRIMARY KEY,
    case_file_id BIGINT NOT NULL,
    seq BIGINT NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload TEXT NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    actor_user_id BIGINT,
    actor_role VARCHAR(60),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_case_file_event_case_file ON tb_case_file_event(case_file_id);
CREATE INDEX IF NOT EXISTS ix_case_file_event_case_file_seq ON tb_case_file_event(case_file_id, seq);
CREATE INDEX IF NOT EXISTS ix_case_file_event_payload_hash ON tb_case_file_event(payload_hash);
