ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS connector_submission_status VARCHAR(80);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS connector_protocol_reference VARCHAR(120);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS connector_submission_message VARCHAR(500);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS connector_submission_processed_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_tb_processo_connector_submission_status ON tb_processo (connector_submission_status);
CREATE INDEX IF NOT EXISTS idx_tb_processo_connector_protocol_reference ON tb_processo (connector_protocol_reference);
CREATE INDEX IF NOT EXISTS idx_tb_processo_connector_submission_processed_at ON tb_processo (connector_submission_processed_at);
