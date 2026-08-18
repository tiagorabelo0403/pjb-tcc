-- Recria tb_outbox_event como tabela particionada por RANGE mensal em created_month (YYYYMM INT).
-- PK: (id, created_month). Partições 2024-01 a 2027-12.
-- Estratégia: tabela vazia em dev — drop + create direto, sem migração de dados.
-- uk_outbox_dedup criado por partição (PG17 não suporta UNIQUE cross-partition sem chave de partição).

DO $$
BEGIN
  IF to_regclass('public.tb_outbox_event') IS NOT NULL THEN
    IF (SELECT COUNT(*) FROM tb_outbox_event) > 0 THEN
      RAISE EXCEPTION 'V295 requer tb_outbox_event vazia. % eventos encontrados. Esvazie a fila (processar/purgar) antes de migrar, ou use estrategia de copia para ambiente com dados.', (SELECT COUNT(*) FROM tb_outbox_event);
    END IF;
  END IF;
END $$;

DROP TABLE IF EXISTS tb_outbox_event;

CREATE TABLE tb_outbox_event (
    id             UUID          NOT NULL,
    created_month  INT           NOT NULL,
    routing_key    VARCHAR(180)  NOT NULL,
    event_type     VARCHAR(120)  NOT NULL,
    payload_json   TEXT          NOT NULL,
    dedup_key      VARCHAR(200),
    aggregate_type VARCHAR(120),
    aggregate_id   VARCHAR(180),
    headers_json   TEXT,
    status         VARCHAR(20)   NOT NULL,
    attempts       INT           NOT NULL DEFAULT 0,
    available_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    locked_by      VARCHAR(120),
    locked_at      TIMESTAMPTZ,
    last_error     TEXT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_outbox_event PRIMARY KEY (id, created_month)
) PARTITION BY RANGE (created_month);

-- Partições 2024
CREATE TABLE tb_outbox_event_y2024m01 PARTITION OF tb_outbox_event FOR VALUES FROM (202401) TO (202402);
CREATE TABLE tb_outbox_event_y2024m02 PARTITION OF tb_outbox_event FOR VALUES FROM (202402) TO (202403);
CREATE TABLE tb_outbox_event_y2024m03 PARTITION OF tb_outbox_event FOR VALUES FROM (202403) TO (202404);
CREATE TABLE tb_outbox_event_y2024m04 PARTITION OF tb_outbox_event FOR VALUES FROM (202404) TO (202405);
CREATE TABLE tb_outbox_event_y2024m05 PARTITION OF tb_outbox_event FOR VALUES FROM (202405) TO (202406);
CREATE TABLE tb_outbox_event_y2024m06 PARTITION OF tb_outbox_event FOR VALUES FROM (202406) TO (202407);
CREATE TABLE tb_outbox_event_y2024m07 PARTITION OF tb_outbox_event FOR VALUES FROM (202407) TO (202408);
CREATE TABLE tb_outbox_event_y2024m08 PARTITION OF tb_outbox_event FOR VALUES FROM (202408) TO (202409);
CREATE TABLE tb_outbox_event_y2024m09 PARTITION OF tb_outbox_event FOR VALUES FROM (202409) TO (202410);
CREATE TABLE tb_outbox_event_y2024m10 PARTITION OF tb_outbox_event FOR VALUES FROM (202410) TO (202411);
CREATE TABLE tb_outbox_event_y2024m11 PARTITION OF tb_outbox_event FOR VALUES FROM (202411) TO (202412);
CREATE TABLE tb_outbox_event_y2024m12 PARTITION OF tb_outbox_event FOR VALUES FROM (202412) TO (202501);

-- Partições 2025
CREATE TABLE tb_outbox_event_y2025m01 PARTITION OF tb_outbox_event FOR VALUES FROM (202501) TO (202502);
CREATE TABLE tb_outbox_event_y2025m02 PARTITION OF tb_outbox_event FOR VALUES FROM (202502) TO (202503);
CREATE TABLE tb_outbox_event_y2025m03 PARTITION OF tb_outbox_event FOR VALUES FROM (202503) TO (202504);
CREATE TABLE tb_outbox_event_y2025m04 PARTITION OF tb_outbox_event FOR VALUES FROM (202504) TO (202505);
CREATE TABLE tb_outbox_event_y2025m05 PARTITION OF tb_outbox_event FOR VALUES FROM (202505) TO (202506);
CREATE TABLE tb_outbox_event_y2025m06 PARTITION OF tb_outbox_event FOR VALUES FROM (202506) TO (202507);
CREATE TABLE tb_outbox_event_y2025m07 PARTITION OF tb_outbox_event FOR VALUES FROM (202507) TO (202508);
CREATE TABLE tb_outbox_event_y2025m08 PARTITION OF tb_outbox_event FOR VALUES FROM (202508) TO (202509);
CREATE TABLE tb_outbox_event_y2025m09 PARTITION OF tb_outbox_event FOR VALUES FROM (202509) TO (202510);
CREATE TABLE tb_outbox_event_y2025m10 PARTITION OF tb_outbox_event FOR VALUES FROM (202510) TO (202511);
CREATE TABLE tb_outbox_event_y2025m11 PARTITION OF tb_outbox_event FOR VALUES FROM (202511) TO (202512);
CREATE TABLE tb_outbox_event_y2025m12 PARTITION OF tb_outbox_event FOR VALUES FROM (202512) TO (202601);

-- Partições 2026
CREATE TABLE tb_outbox_event_y2026m01 PARTITION OF tb_outbox_event FOR VALUES FROM (202601) TO (202602);
CREATE TABLE tb_outbox_event_y2026m02 PARTITION OF tb_outbox_event FOR VALUES FROM (202602) TO (202603);
CREATE TABLE tb_outbox_event_y2026m03 PARTITION OF tb_outbox_event FOR VALUES FROM (202603) TO (202604);
CREATE TABLE tb_outbox_event_y2026m04 PARTITION OF tb_outbox_event FOR VALUES FROM (202604) TO (202605);
CREATE TABLE tb_outbox_event_y2026m05 PARTITION OF tb_outbox_event FOR VALUES FROM (202605) TO (202606);
CREATE TABLE tb_outbox_event_y2026m06 PARTITION OF tb_outbox_event FOR VALUES FROM (202606) TO (202607);
CREATE TABLE tb_outbox_event_y2026m07 PARTITION OF tb_outbox_event FOR VALUES FROM (202607) TO (202608);
CREATE TABLE tb_outbox_event_y2026m08 PARTITION OF tb_outbox_event FOR VALUES FROM (202608) TO (202609);
CREATE TABLE tb_outbox_event_y2026m09 PARTITION OF tb_outbox_event FOR VALUES FROM (202609) TO (202610);
CREATE TABLE tb_outbox_event_y2026m10 PARTITION OF tb_outbox_event FOR VALUES FROM (202610) TO (202611);
CREATE TABLE tb_outbox_event_y2026m11 PARTITION OF tb_outbox_event FOR VALUES FROM (202611) TO (202612);
CREATE TABLE tb_outbox_event_y2026m12 PARTITION OF tb_outbox_event FOR VALUES FROM (202612) TO (202701);

-- Partições 2027
CREATE TABLE tb_outbox_event_y2027m01 PARTITION OF tb_outbox_event FOR VALUES FROM (202701) TO (202702);
CREATE TABLE tb_outbox_event_y2027m02 PARTITION OF tb_outbox_event FOR VALUES FROM (202702) TO (202703);
CREATE TABLE tb_outbox_event_y2027m03 PARTITION OF tb_outbox_event FOR VALUES FROM (202703) TO (202704);
CREATE TABLE tb_outbox_event_y2027m04 PARTITION OF tb_outbox_event FOR VALUES FROM (202704) TO (202705);
CREATE TABLE tb_outbox_event_y2027m05 PARTITION OF tb_outbox_event FOR VALUES FROM (202705) TO (202706);
CREATE TABLE tb_outbox_event_y2027m06 PARTITION OF tb_outbox_event FOR VALUES FROM (202706) TO (202707);
CREATE TABLE tb_outbox_event_y2027m07 PARTITION OF tb_outbox_event FOR VALUES FROM (202707) TO (202708);
CREATE TABLE tb_outbox_event_y2027m08 PARTITION OF tb_outbox_event FOR VALUES FROM (202708) TO (202709);
CREATE TABLE tb_outbox_event_y2027m09 PARTITION OF tb_outbox_event FOR VALUES FROM (202709) TO (202710);
CREATE TABLE tb_outbox_event_y2027m10 PARTITION OF tb_outbox_event FOR VALUES FROM (202710) TO (202711);
CREATE TABLE tb_outbox_event_y2027m11 PARTITION OF tb_outbox_event FOR VALUES FROM (202711) TO (202712);
CREATE TABLE tb_outbox_event_y2027m12 PARTITION OF tb_outbox_event FOR VALUES FROM (202712) TO (202801);

-- Índices no parent — propagam para todas as partições atuais e futuras
CREATE INDEX ix_outbox_status_available ON tb_outbox_event (status, available_at, created_at);
CREATE INDEX ix_outbox_routing_key ON tb_outbox_event (routing_key, created_at);
CREATE INDEX ix_outbox_aggregate ON tb_outbox_event (aggregate_type, aggregate_id, created_at);
CREATE INDEX idx_outbox_failed_created ON tb_outbox_event (created_at) WHERE status = 'FAILED';
CREATE INDEX idx_outbox_inflight_locked ON tb_outbox_event (locked_at, created_at) WHERE status = 'INFLIGHT';
CREATE INDEX idx_outbox_pending_event_type_available ON tb_outbox_event (event_type, available_at, created_at) WHERE status = 'PENDING';

-- uk_outbox_dedup: UNIQUE por partição (PG17 não permite UNIQUE cross-partition sem chave de partição)
DO $$
DECLARE
    r RECORD;
    idx_suffix TEXT;
BEGIN
    FOR r IN
        SELECT inhrelid::regclass::text AS part_name
        FROM pg_inherits
        WHERE inhparent = 'tb_outbox_event'::regclass
        ORDER BY inhrelid
    LOOP
        idx_suffix := replace(replace(r.part_name, 'public.', ''), 'tb_outbox_event_', '');
        EXECUTE format(
            'CREATE UNIQUE INDEX IF NOT EXISTS uk_outbox_dedup_%s ON %s (dedup_key) WHERE dedup_key IS NOT NULL',
            idx_suffix, r.part_name
        );
    END LOOP;
END;
$$;
