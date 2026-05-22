ALTER TABLE tb_laiane_procuracao
    ADD COLUMN IF NOT EXISTS aprovado_por_id BIGINT;

ALTER TABLE tb_laiane_procuracao
    ADD COLUMN IF NOT EXISTS aprovado_em TIMESTAMP;

ALTER TABLE tb_laiane_procuracao
    ADD COLUMN IF NOT EXISTS decisao_motivo TEXT;

ALTER TABLE tb_processo_note
    ADD COLUMN IF NOT EXISTS note_type VARCHAR(32);

ALTER TABLE tb_processo_note
    ADD COLUMN IF NOT EXISTS visible_to_role VARCHAR(64);

ALTER TABLE tb_processo_note
    ADD COLUMN IF NOT EXISTS visible_to_location VARCHAR(128);

ALTER TABLE tb_processo_note
    ADD COLUMN IF NOT EXISTS visible_until TIMESTAMP;

ALTER TABLE tb_processo_note
    ADD COLUMN IF NOT EXISTS due_at TIMESTAMP;

ALTER TABLE tb_processo_note
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP;

ALTER TABLE tb_processo_note
    ADD COLUMN IF NOT EXISTS priority INTEGER;

ALTER TABLE tb_processo_note
    ADD COLUMN IF NOT EXISTS sigilo_nivel VARCHAR(24);

ALTER TABLE tb_secretariat_queue_item
    ADD COLUMN IF NOT EXISTS lane_code VARCHAR(40);

ALTER TABLE tb_secretariat_queue_item
    ADD COLUMN IF NOT EXISTS desk_axis VARCHAR(80);

ALTER TABLE tb_secretariat_queue_item
    ADD COLUMN IF NOT EXISTS workload_band VARCHAR(40);

ALTER TABLE tb_secretariat_queue_item
    ADD COLUMN IF NOT EXISTS routing_fingerprint VARCHAR(240);

ALTER TABLE tb_secretariat_queue_item
    ADD COLUMN IF NOT EXISTS metadata_json TEXT;

ALTER TABLE tb_secretariat_queue_item
    ADD COLUMN IF NOT EXISTS escalation_required BOOLEAN;

UPDATE tb_secretariat_queue_item
   SET escalation_required = false
 WHERE escalation_required IS NULL;

ALTER TABLE tb_secretariat_queue_item
    ALTER COLUMN escalation_required SET NOT NULL;

ALTER TABLE tb_secretariat_queue_item
    ADD COLUMN IF NOT EXISTS secrecy_review_required BOOLEAN;

UPDATE tb_secretariat_queue_item
   SET secrecy_review_required = false
 WHERE secrecy_review_required IS NULL;

ALTER TABLE tb_secretariat_queue_item
    ALTER COLUMN secrecy_review_required SET NOT NULL;

ALTER TABLE tb_secretariat_queue_item
    ADD COLUMN IF NOT EXISTS hearing_sensitive BOOLEAN;

UPDATE tb_secretariat_queue_item
   SET hearing_sensitive = false
 WHERE hearing_sensitive IS NULL;

ALTER TABLE tb_secretariat_queue_item
    ALTER COLUMN hearing_sensitive SET NOT NULL;

ALTER TABLE tb_secretariat_queue_item
    ADD COLUMN IF NOT EXISTS blocking BOOLEAN;

UPDATE tb_secretariat_queue_item
   SET blocking = false
 WHERE blocking IS NULL;

ALTER TABLE tb_secretariat_queue_item
    ALTER COLUMN blocking SET NOT NULL;
