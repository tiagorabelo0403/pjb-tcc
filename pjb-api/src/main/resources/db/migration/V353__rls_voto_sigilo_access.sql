-- F7 (plano de melhoria v3): lote 6.
--
-- tb_sigilo_access_request: processo_id direto, mesmo padrao das satelites (V348/V349).
-- tb_voto_colegiado: um salto via julgamento_id -> tb_julgamento_colegiado.processo_id, mesmo
-- padrao ja usado em tb_acordao (V348).

ALTER TABLE tb_sigilo_access_request ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_sigilo_access_request FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_sigilo_access_request_processo_scope ON tb_sigilo_access_request
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_voto_colegiado ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_voto_colegiado FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_voto_colegiado_processo_scope ON tb_voto_colegiado
    USING (
        EXISTS (
            SELECT 1 FROM tb_julgamento_colegiado j
            WHERE j.id = tb_voto_colegiado.julgamento_id
              AND (j.processo_id IS NULL OR pjb_rls_processo_visivel(j.processo_id))
        )
    )
    WITH CHECK (true);
