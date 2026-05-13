CREATE INDEX IF NOT EXISTS idx_tb_processo_public_workspace_last_move
    ON tb_processo (nivel_sigilo, data_ultima_movimentacao DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_tb_processo_public_workspace_owner
    ON tb_processo (usuario_id, data_ultima_movimentacao DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_tb_processo_public_workspace_cpf_autor
    ON tb_processo (parte_autora_cpf, data_ultima_movimentacao DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_tb_processo_public_workspace_cpf_reu
    ON tb_processo (parte_reu_cpf, data_ultima_movimentacao DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_evento_processual_pending_deadline
    ON evento_processual (responsavel_id, processo_id, status, ativo, data_fim ASC, id ASC);

CREATE INDEX IF NOT EXISTS idx_documento_pagina_page_id_public
    ON tb_documento_pagina (page_id);
