ALTER TABLE tb_processo_event
    ADD COLUMN IF NOT EXISTS prev_chain_hash VARCHAR(96);

ALTER TABLE tb_processo_event
    ADD COLUMN IF NOT EXISTS chain_hash VARCHAR(96);

CREATE INDEX IF NOT EXISTS idx_processo_event_proc_seq ON tb_processo_event(processo_id, seq);
CREATE INDEX IF NOT EXISTS idx_processo_event_proc_payloadhash ON tb_processo_event(processo_id, payload_hash);
CREATE INDEX IF NOT EXISTS idx_processo_event_proc_chainhash ON tb_processo_event(processo_id, chain_hash);
