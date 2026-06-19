-- Recria tb_authz_trail_read_model como tabela particionada por RANGE mensal em occurred_month (YYYYMM INT).
-- PK: (id, occurred_month). id gerado via SEQUENCE seq_authz_trail_id (compatível com @IdClass no Hibernate).
-- Dedup global separada: tb_authz_trail_dedup (PRIMARY KEY(payload_hash)).
-- payload_hash NAO é UNIQUE na trilha — unicidade global fica na tabela de dedup.
-- Estratégia: tabela vazia em dev — drop + create direto, sem migração de dados.
-- Retenção: 10 anos (3650 dias), legal hold, sem purge automático sem guarda explícito.

DO $$
BEGIN
  IF to_regclass('public.tb_authz_trail_read_model') IS NOT NULL THEN
    IF (SELECT COUNT(*) FROM tb_authz_trail_read_model) > 0 THEN
      RAISE EXCEPTION
        'V296 requer tb_authz_trail_read_model vazia. % entradas encontradas. '
        'Esta tabela contém trilha de auditoria ABAC com retencao legal de 10 anos. '
        'Esvazie ou migre os dados antes de aplicar o particionamento.',
        (SELECT COUNT(*) FROM tb_authz_trail_read_model);
    END IF;
  END IF;
END $$;

DROP TABLE IF EXISTS tb_authz_trail_dedup;
DROP TABLE IF EXISTS tb_authz_trail_read_model;
DROP SEQUENCE IF EXISTS seq_authz_trail_id;

CREATE SEQUENCE seq_authz_trail_id START 1 INCREMENT 1;

CREATE TABLE tb_authz_trail_dedup (
    payload_hash   VARCHAR(128)  NOT NULL,
    trail_id       BIGINT,
    occurred_month INT           NOT NULL,
    occurred_at    TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_authz_trail_dedup PRIMARY KEY (payload_hash)
);

CREATE TABLE tb_authz_trail_read_model (
    id                              BIGINT       NOT NULL DEFAULT nextval('seq_authz_trail_id'),
    occurred_month                  INT          NOT NULL,
    occurred_at                     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    audit_event_code                VARCHAR(120) NOT NULL,
    action                          VARCHAR(120) NOT NULL,
    resource_type                   VARCHAR(120) NOT NULL,
    resource_id                     VARCHAR(240) NOT NULL,
    allowed                         BOOLEAN      NOT NULL,
    reason                          VARCHAR(160) NOT NULL,
    policy_version                  VARCHAR(80)  NOT NULL,
    policy_descriptor_sha256        VARCHAR(128) NOT NULL,
    actor_id                        BIGINT,
    actor_type                      VARCHAR(80)  NOT NULL,
    request_id                      VARCHAR(120) NOT NULL,
    justificativa                   TEXT         NOT NULL,
    effective_sigilo                VARCHAR(80)  NOT NULL,
    risk_level                      VARCHAR(40)  NOT NULL,
    risk_score                      INTEGER      NOT NULL,
    step_up_channel                 VARCHAR(80)  NOT NULL,
    step_up_code                    VARCHAR(120) NOT NULL,
    step_up_required                BOOLEAN      NOT NULL,
    step_up_satisfied               BOOLEAN      NOT NULL,
    governance_channel              VARCHAR(80)  NOT NULL,
    governance_code                 VARCHAR(120) NOT NULL,
    governance_scope                VARCHAR(120) NOT NULL,
    governance_required             BOOLEAN      NOT NULL,
    governance_satisfied            BOOLEAN      NOT NULL,
    integration_code                VARCHAR(120) NOT NULL,
    institutional_unit_code         VARCHAR(120) NOT NULL,
    institutional_box_code          VARCHAR(120) NOT NULL,
    institutional_capability_code   VARCHAR(120) NOT NULL,
    expedicao_uuid                  VARCHAR(120) NOT NULL,
    payload_hash                    VARCHAR(128) NOT NULL,
    audit_description               TEXT         NOT NULL,
    CONSTRAINT pk_authz_trail PRIMARY KEY (id, occurred_month)
) PARTITION BY RANGE (occurred_month);

ALTER SEQUENCE seq_authz_trail_id OWNED BY tb_authz_trail_read_model.id;

-- Partições 2024
CREATE TABLE tb_authz_trail_y2024m01 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202401) TO (202402);
CREATE TABLE tb_authz_trail_y2024m02 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202402) TO (202403);
CREATE TABLE tb_authz_trail_y2024m03 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202403) TO (202404);
CREATE TABLE tb_authz_trail_y2024m04 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202404) TO (202405);
CREATE TABLE tb_authz_trail_y2024m05 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202405) TO (202406);
CREATE TABLE tb_authz_trail_y2024m06 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202406) TO (202407);
CREATE TABLE tb_authz_trail_y2024m07 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202407) TO (202408);
CREATE TABLE tb_authz_trail_y2024m08 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202408) TO (202409);
CREATE TABLE tb_authz_trail_y2024m09 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202409) TO (202410);
CREATE TABLE tb_authz_trail_y2024m10 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202410) TO (202411);
CREATE TABLE tb_authz_trail_y2024m11 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202411) TO (202412);
CREATE TABLE tb_authz_trail_y2024m12 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202412) TO (202501);

-- Partições 2025
CREATE TABLE tb_authz_trail_y2025m01 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202501) TO (202502);
CREATE TABLE tb_authz_trail_y2025m02 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202502) TO (202503);
CREATE TABLE tb_authz_trail_y2025m03 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202503) TO (202504);
CREATE TABLE tb_authz_trail_y2025m04 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202504) TO (202505);
CREATE TABLE tb_authz_trail_y2025m05 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202505) TO (202506);
CREATE TABLE tb_authz_trail_y2025m06 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202506) TO (202507);
CREATE TABLE tb_authz_trail_y2025m07 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202507) TO (202508);
CREATE TABLE tb_authz_trail_y2025m08 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202508) TO (202509);
CREATE TABLE tb_authz_trail_y2025m09 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202509) TO (202510);
CREATE TABLE tb_authz_trail_y2025m10 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202510) TO (202511);
CREATE TABLE tb_authz_trail_y2025m11 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202511) TO (202512);
CREATE TABLE tb_authz_trail_y2025m12 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202512) TO (202601);

-- Partições 2026
CREATE TABLE tb_authz_trail_y2026m01 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202601) TO (202602);
CREATE TABLE tb_authz_trail_y2026m02 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202602) TO (202603);
CREATE TABLE tb_authz_trail_y2026m03 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202603) TO (202604);
CREATE TABLE tb_authz_trail_y2026m04 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202604) TO (202605);
CREATE TABLE tb_authz_trail_y2026m05 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202605) TO (202606);
CREATE TABLE tb_authz_trail_y2026m06 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202606) TO (202607);
CREATE TABLE tb_authz_trail_y2026m07 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202607) TO (202608);
CREATE TABLE tb_authz_trail_y2026m08 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202608) TO (202609);
CREATE TABLE tb_authz_trail_y2026m09 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202609) TO (202610);
CREATE TABLE tb_authz_trail_y2026m10 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202610) TO (202611);
CREATE TABLE tb_authz_trail_y2026m11 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202611) TO (202612);
CREATE TABLE tb_authz_trail_y2026m12 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202612) TO (202701);

-- Partições 2027
CREATE TABLE tb_authz_trail_y2027m01 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202701) TO (202702);
CREATE TABLE tb_authz_trail_y2027m02 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202702) TO (202703);
CREATE TABLE tb_authz_trail_y2027m03 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202703) TO (202704);
CREATE TABLE tb_authz_trail_y2027m04 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202704) TO (202705);
CREATE TABLE tb_authz_trail_y2027m05 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202705) TO (202706);
CREATE TABLE tb_authz_trail_y2027m06 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202706) TO (202707);
CREATE TABLE tb_authz_trail_y2027m07 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202707) TO (202708);
CREATE TABLE tb_authz_trail_y2027m08 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202708) TO (202709);
CREATE TABLE tb_authz_trail_y2027m09 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202709) TO (202710);
CREATE TABLE tb_authz_trail_y2027m10 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202710) TO (202711);
CREATE TABLE tb_authz_trail_y2027m11 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202711) TO (202712);
CREATE TABLE tb_authz_trail_y2027m12 PARTITION OF tb_authz_trail_read_model FOR VALUES FROM (202712) TO (202801);

-- Índices no parent propagam para todas as partições (PG10+)
CREATE INDEX idx_authz_trail_occurred      ON tb_authz_trail_read_model (occurred_at DESC);
CREATE INDEX idx_authz_trail_action_resource ON tb_authz_trail_read_model (action, resource_type, occurred_at DESC);
CREATE INDEX idx_authz_trail_actor         ON tb_authz_trail_read_model (actor_id, occurred_at DESC);
CREATE INDEX idx_authz_trail_request       ON tb_authz_trail_read_model (request_id);
CREATE INDEX idx_authz_trail_integration   ON tb_authz_trail_read_model (integration_code, occurred_at DESC);
CREATE INDEX idx_authz_trail_institutional ON tb_authz_trail_read_model (institutional_unit_code, institutional_box_code, institutional_capability_code, occurred_at DESC);

CREATE INDEX brin_authz_trail_occurred ON tb_authz_trail_read_model USING brin (occurred_at);

INSERT INTO tb_database_retention_policy (table_name, retention_window_days, archive_strategy, purge_mode, legal_hold_supported, notes)
VALUES ('tb_authz_trail_dedup', 3650, 'KEEP_PRIMARY_AUDIT', 'NO_PURGE', TRUE, 'Tabela de dedup global da trilha de autorizacao ABAC. Unicidade de payload_hash cross-partition.')
ON CONFLICT (table_name)
DO UPDATE SET
    retention_window_days = EXCLUDED.retention_window_days,
    archive_strategy      = EXCLUDED.archive_strategy,
    purge_mode            = EXCLUDED.purge_mode,
    legal_hold_supported  = EXCLUDED.legal_hold_supported,
    notes                 = EXCLUDED.notes,
    updated_at            = NOW();

INSERT INTO tb_database_retention_policy (table_name, retention_window_days, archive_strategy, purge_mode, legal_hold_supported, notes)
VALUES ('tb_authz_trail_read_model', 3650, 'KEEP_PRIMARY_AUDIT', 'NO_PURGE', TRUE, 'Read model particionado por occurred_month da trilha de autorizacao ABAC. Purge O(1) por DROP PARTITION após legal hold.')
ON CONFLICT (table_name)
DO UPDATE SET
    retention_window_days = EXCLUDED.retention_window_days,
    archive_strategy      = EXCLUDED.archive_strategy,
    purge_mode            = EXCLUDED.purge_mode,
    legal_hold_supported  = EXCLUDED.legal_hold_supported,
    notes                 = EXCLUDED.notes,
    updated_at            = NOW();
