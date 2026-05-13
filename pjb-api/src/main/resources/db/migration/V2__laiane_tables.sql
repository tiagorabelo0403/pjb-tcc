-- Laiane (assistente interno do PJB)
-- Tabelas auxiliares para: preferências, pacotes de protocolo e fingerprints.

CREATE TABLE IF NOT EXISTS tb_laiane_user_preference (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    preferences_json TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_laiane_user_pref_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

CREATE TABLE IF NOT EXISTS tb_laiane_protocol_package (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    title VARCHAR(255),
    payload_json TEXT NOT NULL,
    integrity_hash VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_laiane_protocol_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_laiane_protocol_usuario_created ON tb_laiane_protocol_package(usuario_id, created_at DESC);

CREATE TABLE IF NOT EXISTS tb_laiane_document_fingerprint (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    sha256 VARCHAR(64) NOT NULL,
    mime VARCHAR(128),
    size_bytes BIGINT,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_laiane_fingerprint_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_laiane_fingerprint_user_sha ON tb_laiane_document_fingerprint(usuario_id, sha256);
