ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS objeto_processual VARCHAR(240);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS pedido_principal VARCHAR(240);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS pedidos_consolidados TEXT;
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS material_probatorio_resumo TEXT;
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS material_probatorio_hash VARCHAR(64);
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS material_probatorio_score INT;
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS potencial_acordo_score INT;
ALTER TABLE tb_processo ADD COLUMN IF NOT EXISTS janela_acordo_resumo VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_processo_material_probatorio_hash
    ON tb_processo (material_probatorio_hash);

CREATE INDEX IF NOT EXISTS idx_processo_potencial_acordo_score
    ON tb_processo (potencial_acordo_score);
