-- F7 (plano de melhoria v3): estende o backstop de RLS por ator/papel (padrao V343) para mais duas
-- tabelas de conteudo sensivel que ainda estavam sem protecao no banco.
--
-- tb_credencial_acesso: login + hash de senha de credencial de acesso externo a um processo. Não
-- existe fluxo de usuario comum "navegando" suas proprias credenciais (CredencialAcessoService so e
-- consumido pelo fluxo de validacao da propria credencial, tipicamente anonimo/pre-autenticacao, e
-- por administracao). Policy: contexto de sistema (inclui o anonimo que ainda nao se autenticou, o
-- caso real de uso) ou administrador.
--
-- tb_boletim_ocorrencia_digital: registro de ocorrencia policial. BoletimOcorrenciaDigitalController
-- exige DELEGADO_POLICIA / DELEGADO_POLICIA_FEDERAL / AGENTE_POLICIAL / ESCRIVAO_POLICIAL em todos os
-- endpoints (@PreAuthorize). Policy espelha exatamente esses papeis, mesmo idioma de
-- intimacao_audiencia_reader_scope.
--
-- As duas usam os helpers de V343 (pjb_rls_system_context/has_role), ja com GUCs app.pjb_actor_id /
-- app.pjb_actor_roles preenchidas em toda conexao real via PjbProcessoSigiloRlsDataSource. WITH CHECK
-- (true): RLS aqui e backstop de LEITURA sobre o @PreAuthorize, nao substituto dele.

ALTER TABLE tb_credencial_acesso ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_credencial_acesso FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_credencial_acesso_system_admin_scope ON tb_credencial_acesso
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
    )
    WITH CHECK (true);

ALTER TABLE tb_boletim_ocorrencia_digital ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_boletim_ocorrencia_digital FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_boletim_ocorrencia_digital_papel_scope ON tb_boletim_ocorrencia_digital
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR pjb_rls_has_role('ROLE_DELEGADO_POLICIA')
        OR pjb_rls_has_role('ROLE_DELEGADO_POLICIA_FEDERAL')
        OR pjb_rls_has_role('ROLE_AGENTE_POLICIAL')
        OR pjb_rls_has_role('ROLE_ESCRIVAO_POLICIAL')
    )
    WITH CHECK (true);
