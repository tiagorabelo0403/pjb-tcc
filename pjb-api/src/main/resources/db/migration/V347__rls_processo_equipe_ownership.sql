-- F7 (plano de melhoria v3): RLS em tb_processo, espelhando o MESMO @Filter Hibernate
-- "filtroEquipeProcesso" que ja protege toda consulta normal (ver Processo.java) — nao uma
-- aproximacao por papel, mas a mesma checagem de ownership (usuario_id/equipe_id) usando as
-- MESMAS colunas.
--
-- O @Filter so e habilitado pelo EquipeSwitchInterceptor para atores tipo-advogado
-- (Usuario.isAdvogado()) — para qualquer outro papel (juiz, servidor, MP, etc.) ele nunca liga,
-- e a tabela fica sem restricao de ownership nesse caminho (a autorizacao deles, se houver, e
-- outro mecanismo, fora do escopo desta policy). A policy replica esse mesmo comportamento: só
-- restringe quando app.pjb_equipe_filter_active = 'true' (GUC publicada pelo mesmo
-- PjbProcessoSigiloRlsDataSource que ja publica app.pjb_actor_id, agora tambem lendo
-- EquipeFiltroContexto via PjbRlsEquipeResolver).
--
-- Este e o backstop de OWNERSHIP (isolamento entre escritorios/equipes de advocacia). Nao repete
-- as dimensoes de ramo_direito/nivel_sigilo/bloqueio de caso pessoal do @Filter (allowRamos,
-- allowSensitive, blockPersonalCases) — essas ficam, por ora, so na camada de aplicacao. RLS aqui
-- e defesa em profundidade sobre o Hibernate, nao substituto dele.

ALTER TABLE tb_processo ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_processo FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_processo_equipe_ownership_scope ON tb_processo
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR COALESCE(current_setting('app.pjb_equipe_filter_active', true), 'false') <> 'true'
        OR (equipe_id IS NOT NULL AND equipe_id::text = current_setting('app.pjb_equipe_id', true))
        OR usuario_id::text = current_setting('app.pjb_equipe_usuario_id', true)
    )
    WITH CHECK (true);
