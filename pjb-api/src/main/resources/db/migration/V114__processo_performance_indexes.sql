CREATE INDEX IF NOT EXISTS idx_processo_status
    ON tb_processo (status_processo);

CREATE INDEX IF NOT EXISTS idx_processo_ramo_status
    ON tb_processo (ramo_direito, status_processo);

CREATE INDEX IF NOT EXISTS idx_processo_ultima_mov
    ON tb_processo (data_ultima_movimentacao DESC NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_processo_fase_status
    ON tb_processo (fase_atual, status_processo);
