-- F7 (plano de melhoria v3): lote 5.
--
-- tb_marketplace_client_app, tb_marketplace_webhook_endpoint: sem dono individual (app parceiro /
-- endpoint de callback). ApiMarketplaceOAuthController (/clients) e ApiMarketplaceAdminController
-- (classe inteira, incl. /clients/{id}/webhooks) exigem ADMINISTRADOR, SERVIDOR ou SERVIDOR_FORUM
-- -- nao so admin -- mesmo padrao de tb_marketplace_access_token (V350).
--
-- tb_pessoa_localizacao_consulta: log de auditoria de consulta de localizacao de pessoa (dado
-- extremamente sensivel -- rastreamento). executor_user_id identifica quem executou a consulta;
-- policy espelha o padrao de ownership ja usado (V350) -- o proprio executor ve suas consultas,
-- admin ve tudo, ninguem mais.

ALTER TABLE tb_marketplace_client_app ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_marketplace_client_app FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_marketplace_client_app_system_admin_scope ON tb_marketplace_client_app
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR pjb_rls_has_role('ROLE_SERVIDOR')
        OR pjb_rls_has_role('ROLE_SERVIDOR_FORUM')
    )
    WITH CHECK (true);

ALTER TABLE tb_marketplace_webhook_endpoint ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_marketplace_webhook_endpoint FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_marketplace_webhook_endpoint_system_admin_scope ON tb_marketplace_webhook_endpoint
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR pjb_rls_has_role('ROLE_SERVIDOR')
        OR pjb_rls_has_role('ROLE_SERVIDOR_FORUM')
    )
    WITH CHECK (true);

ALTER TABLE tb_pessoa_localizacao_consulta ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_pessoa_localizacao_consulta FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_pessoa_localizacao_consulta_executor_scope ON tb_pessoa_localizacao_consulta
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR executor_user_id::text = pjb_rls_actor_id()
    )
    WITH CHECK (true);
