-- F7 (plano de melhoria v3): lote 3 -- tabelas de credencial/sessao/step-up ligadas a um usuario
-- especifico. Diferente do lote de tb_processo (ownership por equipe), aqui a policy espelha o
-- padrao ja estabelecido na V343 (aberto_por_id/usuario_id::text = pjb_rls_actor_id()) porque cada
-- uma tem dono direto e usuarios legitimamente precisam ler/gerenciar sua PROPRIA linha
-- (autoatendimento: gerenciar as proprias passkeys, desbloquear a propria funcao operacional,
-- vincular o proprio gov.br) -- restringir a admin/sistema quebraria esses fluxos.
--
-- tb_marketplace_access_token NAO tem dono individual (token de app parceiro). /token e
-- /introspect (ApiMarketplaceOAuthController) sao publicos -- authorizeBearerToken/
-- authorizeHttpRequest rodam fiados no SecurityConfig como filtro de resolucao de identidade,
-- ANTES de qualquer ator PJB estar estabelecido na conexao, cobertos por pjb_rls_system_context().
-- Mas /revoke exige ADMINISTRADOR/SERVIDOR/SERVIDOR_FORUM (nao so admin) e chama
-- MarketplaceOAuth2Service.revogar(), que le a linha do token sob o ator ja autenticado -- por
-- isso a policy inclui esses dois papeis, nao só admin.
--
-- pjb_icp_trust_anchor NAO entra nesta migration -- ver achado da revisao profunda abaixo.
--
-- ACHADO (revisao profunda, nao aplicado): pjb_icp_trust_anchor foi cogitada para system+admin
-- only, mas IcpBrasilChainValidator.validate() chama activeAnchorCount() como GATE de validacao
-- ("if (enforceChainValidation() && activeAnchorCount() == 0) return fail(...)"), disparado por
-- RecursalFormalizacaoService -> RecursalPdfProofEnvelopeService -> RecursalIcpBrasilIntegrationService
-- quando um ADVOGADO JA AUTENTICADO (nao admin) formaliza um recurso com o proprio certificado.
-- Restringir essa tabela faria a contagem retornar 0 pra esse ator e a assinatura digital falhar
-- silenciosamente assim que PJB_ICP_ENFORCE_CHAIN (hoje false) for ligado -- quebrando exatamente
-- o requisito de validade juridica que a F10 travou. Alem disso, o conteudo (DN/serial/certificado
-- DER de uma Autoridade Certificadora) e informacao publica de infraestrutura de chave publica, nao
-- segredo -- restringir a leitura nao protege nada real. Tabela deliberadamente fora do escopo do RLS.

ALTER TABLE operational_function_credentials ENABLE ROW LEVEL SECURITY;
ALTER TABLE operational_function_credentials FORCE ROW LEVEL SECURITY;
CREATE POLICY operational_function_credentials_owner_scope ON operational_function_credentials
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR usuario_id::text = pjb_rls_actor_id()
    )
    WITH CHECK (true);

ALTER TABLE operational_function_unlock_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE operational_function_unlock_sessions FORCE ROW LEVEL SECURITY;
CREATE POLICY operational_function_unlock_sessions_owner_scope ON operational_function_unlock_sessions
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR usuario_id::text = pjb_rls_actor_id()
    )
    WITH CHECK (true);

ALTER TABLE passkey_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE passkey_sessions FORCE ROW LEVEL SECURITY;
CREATE POLICY passkey_sessions_owner_scope ON passkey_sessions
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR usuario_id::text = pjb_rls_actor_id()
    )
    WITH CHECK (true);

ALTER TABLE tb_decision_stepup_consumption ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_decision_stepup_consumption FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_decision_stepup_consumption_owner_scope ON tb_decision_stepup_consumption
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR usuario_id::text = pjb_rls_actor_id()
    )
    WITH CHECK (true);

ALTER TABLE tb_govbr_link_state ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_govbr_link_state FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_govbr_link_state_owner_scope ON tb_govbr_link_state
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR usuario_id::text = pjb_rls_actor_id()
    )
    WITH CHECK (true);

ALTER TABLE tb_govbr_stepup_state ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_govbr_stepup_state FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_govbr_stepup_state_owner_scope ON tb_govbr_stepup_state
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR usuario_id::text = pjb_rls_actor_id()
    )
    WITH CHECK (true);

ALTER TABLE tb_marketplace_access_token ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_marketplace_access_token FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_marketplace_access_token_system_admin_scope ON tb_marketplace_access_token
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR pjb_rls_has_role('ROLE_SERVIDOR')
        OR pjb_rls_has_role('ROLE_SERVIDOR_FORUM')
    )
    WITH CHECK (true);
