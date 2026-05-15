CREATE INDEX IF NOT EXISTS idx_tb_processo_sort_movimentacao
    ON tb_processo (
        COALESCE(data_ultima_movimentacao, data_atualizacao, data_criacao) DESC NULLS LAST,
        id DESC
    );

CREATE INDEX IF NOT EXISTS idx_tb_work_item_sort_updated
    ON tb_work_item (
        COALESCE(updated_at, created_at) DESC NULLS LAST,
        created_at DESC,
        id DESC
    );
