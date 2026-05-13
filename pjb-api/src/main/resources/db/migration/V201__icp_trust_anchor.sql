CREATE TABLE pjb_icp_trust_anchor (
 id BIGSERIAL PRIMARY KEY,
 subject_dn TEXT NOT NULL,
 issuer_dn TEXT NOT NULL,
 serial_hex VARCHAR(128) NOT NULL,
 ac_sigla VARCHAR(64) NOT NULL,
 ativo BOOLEAN NOT NULL DEFAULT TRUE,
 certificado_der BYTEA NOT NULL,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 updated_at TIMESTAMPTZ,
 CONSTRAINT uk_icp_anchor_serial UNIQUE (issuer_dn, serial_hex)
);
CREATE INDEX idx_icp_anchor_ativo ON pjb_icp_trust_anchor (ativo) WHERE ativo = TRUE;
CREATE INDEX idx_icp_anchor_sigla ON pjb_icp_trust_anchor (ac_sigla);
