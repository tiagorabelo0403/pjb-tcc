-- F7 (plano de melhoria v3): lote 4.
--
-- tb_inquerito_policial_digital, tb_defensoria_vulnerabilidade_caso: espelham exatamente os
-- papeis do @PreAuthorize dos respectivos controllers (InqueritoPolicialDigitalController,
-- DefensoriaVulnerabilidadeController), mesmo idioma da V346 (tb_boletim_ocorrencia_digital).
--
-- adv_clientes: mesma logica de ownership de tb_processo/equipe (advogado_id/equipe_id), so que
-- o @Filter aqui e' o "filtroEquipe" da Cliente.java, nao "filtroEquipeProcesso" -- reaproveita as
-- MESMAS GUCs (app.pjb_equipe_filter_active/usuario_id/equipe_id) porque sao publicadas pelo
-- mesmo EquipeFiltroContexto, independente de qual entidade a esta' consumindo.

ALTER TABLE tb_inquerito_policial_digital ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_inquerito_policial_digital FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_inquerito_policial_digital_papel_scope ON tb_inquerito_policial_digital
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR pjb_rls_has_role('ROLE_DELEGADO_POLICIA')
        OR pjb_rls_has_role('ROLE_DELEGADO_POLICIA_FEDERAL')
        OR pjb_rls_has_role('ROLE_AGENTE_POLICIAL')
        OR pjb_rls_has_role('ROLE_ESCRIVAO_POLICIAL')
        OR pjb_rls_has_role('ROLE_MEMBRO_MINISTERIO_PUBLICO')
        OR pjb_rls_has_role('ROLE_JUIZ')
        OR pjb_rls_has_role('ROLE_JUIZ_ESTADUAL')
        OR pjb_rls_has_role('ROLE_JUIZ_FEDERAL')
        OR pjb_rls_has_role('ROLE_JUIZ_MILITAR')
        OR pjb_rls_has_role('ROLE_MINISTRO')
    )
    WITH CHECK (true);

ALTER TABLE tb_defensoria_vulnerabilidade_caso ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_defensoria_vulnerabilidade_caso FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_defensoria_vulnerabilidade_caso_papel_scope ON tb_defensoria_vulnerabilidade_caso
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR pjb_rls_has_role('ROLE_DEFENSOR_PUBLICO')
        OR pjb_rls_has_role('ROLE_DEFENSOR_PUBLICO_FEDERAL')
    )
    WITH CHECK (true);

ALTER TABLE adv_clientes ENABLE ROW LEVEL SECURITY;
ALTER TABLE adv_clientes FORCE ROW LEVEL SECURITY;
CREATE POLICY adv_clientes_equipe_ownership_scope ON adv_clientes
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR COALESCE(current_setting('app.pjb_equipe_filter_active', true), 'false') <> 'true'
        OR (equipe_id IS NOT NULL AND equipe_id::text = current_setting('app.pjb_equipe_id', true))
        OR advogado_id::text = current_setting('app.pjb_equipe_usuario_id', true)
    )
    WITH CHECK (true);
