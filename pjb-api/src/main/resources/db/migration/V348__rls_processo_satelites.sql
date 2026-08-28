-- F7 (plano de melhoria v3): estende o backstop de ownership de tb_processo (V347) para as
-- tabelas-satelite que penduram diretamente nela, via EXISTS contra a MESMA regra — sem duplicar
-- a logica de ownership em cada tabela.
--
-- tb_documento_processual, tb_polo_processual, tb_audiencia: processo_id direto.
-- tb_acordao: um salto via julgamento_id -> tb_julgamento_colegiado.processo_id.
--
-- tb_audiencia.processo_id e' nullable (schema historico); quando nulo, permissivo — nao ha
-- processo pra restringir contra. As demais tem processo_id NOT NULL.

CREATE OR REPLACE FUNCTION pjb_rls_processo_visivel(p_processo_id bigint) RETURNS boolean
LANGUAGE sql STABLE AS $$
    SELECT EXISTS (
        SELECT 1 FROM tb_processo p
        WHERE p.id = p_processo_id
          AND (
              pjb_rls_system_context()
              OR pjb_rls_has_role('ROLE_ADMIN')
              OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
              OR COALESCE(current_setting('app.pjb_equipe_filter_active', true), 'false') <> 'true'
              OR (p.equipe_id IS NOT NULL AND p.equipe_id::text = current_setting('app.pjb_equipe_id', true))
              OR p.usuario_id::text = current_setting('app.pjb_equipe_usuario_id', true)
          )
    )
$$;

ALTER TABLE tb_documento_processual ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_documento_processual FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_documento_processual_processo_scope ON tb_documento_processual
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_polo_processual ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_polo_processual FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_polo_processual_processo_scope ON tb_polo_processual
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_audiencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_audiencia FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_audiencia_processo_scope ON tb_audiencia
    USING (processo_id IS NULL OR pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_acordao ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_acordao FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_acordao_processo_scope ON tb_acordao
    USING (
        EXISTS (
            SELECT 1 FROM tb_julgamento_colegiado j
            WHERE j.id = tb_acordao.julgamento_id
              AND (j.processo_id IS NULL OR pjb_rls_processo_visivel(j.processo_id))
        )
    )
    WITH CHECK (true);
