CREATE INDEX IF NOT EXISTS idx_pessoa_localizacao_exec_canal_created
    ON tb_pessoa_localizacao_consulta(executor_user_id, canal_consulta, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_pessoa_localizacao_exec_canal_review
    ON tb_pessoa_localizacao_consulta(executor_user_id, canal_consulta, requer_revisao, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_pessoa_localizacao_exec_canal_stepup
    ON tb_pessoa_localizacao_consulta(executor_user_id, canal_consulta, step_up_required, step_up_satisfied, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_proc_leitura_signal
    ON tb_processo_leitura_ator(processo_id, actor_cluster, last_party_signal_at DESC, last_read_at DESC);
