CREATE TABLE IF NOT EXISTS tb_legal_ai_knowledge_source (
    id BIGSERIAL PRIMARY KEY,
    row_version BIGINT NOT NULL DEFAULT 0,
    source_id VARCHAR(120) NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_kind VARCHAR(80) NOT NULL,
    authority_level VARCHAR(80) NOT NULL,
    institution VARCHAR(160) NOT NULL,
    storage_lane VARCHAR(80) NOT NULL,
    licensing_model VARCHAR(80) NOT NULL,
    base_url VARCHAR(600),
    refresh_strategy VARCHAR(80) NOT NULL,
    branch_codes_json TEXT NOT NULL,
    artifact_types_json TEXT NOT NULL,
    retrieval_tags_json TEXT NOT NULL,
    restrictions_json TEXT NOT NULL,
    metadata_json TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    version_tag VARCHAR(80) NOT NULL,
    official_source BOOLEAN NOT NULL DEFAULT FALSE,
    doctrine_source BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    artifact_count INTEGER NOT NULL DEFAULT 0,
    revision_count INTEGER NOT NULL DEFAULT 0,
    last_synced_at TIMESTAMPTZ,
    next_refresh_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_legal_ai_knowledge_source_source_id UNIQUE (source_id)
);

CREATE INDEX IF NOT EXISTS idx_legal_ai_knowledge_source_lane
    ON tb_legal_ai_knowledge_source (storage_lane, active);

CREATE INDEX IF NOT EXISTS idx_legal_ai_knowledge_source_refresh
    ON tb_legal_ai_knowledge_source (refresh_strategy, next_refresh_at);

CREATE INDEX IF NOT EXISTS idx_legal_ai_knowledge_source_branch_summary
    ON tb_legal_ai_knowledge_source (institution, authority_level);

CREATE TABLE IF NOT EXISTS tb_legal_ai_knowledge_revision (
    id BIGSERIAL PRIMARY KEY,
    source_ref_id BIGINT NOT NULL,
    revision_key VARCHAR(96) NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    revision_status VARCHAR(40) NOT NULL,
    artifact_count INTEGER NOT NULL DEFAULT 0,
    manifest_json TEXT NOT NULL,
    harvested_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_legal_ai_knowledge_revision_source FOREIGN KEY (source_ref_id) REFERENCES tb_legal_ai_knowledge_source (id) ON DELETE CASCADE,
    CONSTRAINT uk_legal_ai_knowledge_revision_key UNIQUE (source_ref_id, revision_key)
);

CREATE INDEX IF NOT EXISTS idx_legal_ai_knowledge_revision_source
    ON tb_legal_ai_knowledge_revision (source_ref_id, harvested_at DESC);

CREATE INDEX IF NOT EXISTS idx_legal_ai_knowledge_revision_status
    ON tb_legal_ai_knowledge_revision (revision_status, harvested_at DESC);

CREATE TABLE IF NOT EXISTS tb_legal_ai_knowledge_artifact (
    id BIGSERIAL PRIMARY KEY,
    source_ref_id BIGINT NOT NULL,
    revision_ref_id BIGINT,
    artifact_key VARCHAR(160) NOT NULL,
    external_id VARCHAR(160),
    branch_code VARCHAR(80),
    artifact_type VARCHAR(80) NOT NULL,
    storage_lane VARCHAR(80) NOT NULL,
    authority_level VARCHAR(80) NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_url VARCHAR(700),
    excerpt TEXT,
    content_hash VARCHAR(64) NOT NULL,
    effective_date DATE,
    metadata_json TEXT NOT NULL,
    CONSTRAINT fk_legal_ai_knowledge_artifact_source FOREIGN KEY (source_ref_id) REFERENCES tb_legal_ai_knowledge_source (id) ON DELETE CASCADE,
    CONSTRAINT fk_legal_ai_knowledge_artifact_revision FOREIGN KEY (revision_ref_id) REFERENCES tb_legal_ai_knowledge_revision (id) ON DELETE SET NULL,
    CONSTRAINT uk_legal_ai_knowledge_artifact_key UNIQUE (source_ref_id, artifact_key)
);

CREATE INDEX IF NOT EXISTS idx_legal_ai_knowledge_artifact_source
    ON tb_legal_ai_knowledge_artifact (source_ref_id, artifact_type);

CREATE INDEX IF NOT EXISTS idx_legal_ai_knowledge_artifact_branch
    ON tb_legal_ai_knowledge_artifact (branch_code, storage_lane);

CREATE INDEX IF NOT EXISTS idx_legal_ai_knowledge_artifact_revision
    ON tb_legal_ai_knowledge_artifact (revision_ref_id);
