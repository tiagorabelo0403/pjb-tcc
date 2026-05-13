CREATE TABLE pjb_icp_certificate_cache (
 id BIGSERIAL PRIMARY KEY,
 subject_dn TEXT NOT NULL,
 issuer_dn TEXT NOT NULL,
 serial_hex VARCHAR(128) NOT NULL,
 cpf_titular VARCHAR(11),
 cnpj_titular VARCHAR(14),
 cert_type VARCHAR(8) NOT NULL,
 ac_sigla VARCHAR(64),
 valid_from TIMESTAMPTZ NOT NULL,
 valid_until TIMESTAMPTZ NOT NULL,
 revoked BOOLEAN NOT NULL DEFAULT FALSE,
 revocation_checked TIMESTAMPTZ,
 raw_der BYTEA,
 created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 CONSTRAINT uk_icp_cert_serial UNIQUE (issuer_dn, serial_hex)
);
CREATE INDEX idx_icp_cert_cpf ON pjb_icp_certificate_cache (cpf_titular) WHERE cpf_titular IS NOT NULL;
CREATE INDEX idx_icp_cert_cnpj ON pjb_icp_certificate_cache (cnpj_titular) WHERE cnpj_titular IS NOT NULL;
CREATE INDEX idx_icp_cert_valid ON pjb_icp_certificate_cache (valid_until) WHERE revoked = FALSE;

CREATE TABLE pjb_icp_signature_event (
 id BIGSERIAL PRIMARY KEY,
 processo_id BIGINT REFERENCES tb_processo(id) ON DELETE SET NULL,
 doc_hash VARCHAR(128) NOT NULL,
 cert_serial_hex VARCHAR(128),
 cpf_signatario VARCHAR(11),
 profile_candidate VARCHAR(8) NOT NULL,
 profile_achieved VARCHAR(8),
 ts_rfc3161_embedded BOOLEAN NOT NULL DEFAULT FALSE,
 dss_dict_present BOOLEAN NOT NULL DEFAULT FALSE,
 vri_present BOOLEAN NOT NULL DEFAULT FALSE,
 signed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
 validation_ok BOOLEAN,
 failure_reason TEXT
);
CREATE INDEX idx_icp_sig_evento_processo ON pjb_icp_signature_event (processo_id);
CREATE INDEX idx_icp_sig_evento_cpf ON pjb_icp_signature_event (cpf_signatario);
