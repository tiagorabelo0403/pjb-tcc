ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS tribunal_codigo_roteado VARCHAR(30);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS classe_tpu_codigo VARCHAR(30);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS unidade_judiciaria_codigo VARCHAR(80);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS competencia_territorial_modo VARCHAR(60);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS prevention_mode VARCHAR(60);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS linkage_mode VARCHAR(60);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS connector_system VARCHAR(40);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS pre_protocolo_status VARCHAR(60);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS submission_blueprint_status VARCHAR(80);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS submission_dry_run_status VARCHAR(80);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS routing_risk_level VARCHAR(30);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS routing_confidence NUMERIC(7,4);

CREATE INDEX IF NOT EXISTS idx_tb_processo_tribunal_codigo_roteado ON tb_processo (tribunal_codigo_roteado);
CREATE INDEX IF NOT EXISTS idx_tb_processo_classe_tpu_codigo ON tb_processo (classe_tpu_codigo);
CREATE INDEX IF NOT EXISTS idx_tb_processo_unidade_judiciaria_codigo ON tb_processo (unidade_judiciaria_codigo);
CREATE INDEX IF NOT EXISTS idx_tb_processo_submission_blueprint_status ON tb_processo (submission_blueprint_status);
