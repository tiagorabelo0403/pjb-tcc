CREATE TABLE pjb_datajud_feed_checkpoint (
 id BIGSERIAL PRIMARY KEY,
 tribunal_codigo VARCHAR(32) NOT NULL UNIQUE,
 last_processo_id BIGINT NOT NULL DEFAULT 0,
 last_sent_at TIMESTAMPTZ,
 total_sent BIGINT NOT NULL DEFAULT 0,
 last_error TEXT,
 updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
