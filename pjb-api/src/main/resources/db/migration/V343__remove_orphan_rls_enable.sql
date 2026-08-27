-- Higiene + blindagem de RLS.
--
-- Sete tabelas tinham Row Level Security habilitado em migrations antigas, mas sem policy e sem
-- FORCE. Em PostgreSQL isso é ignorado pelo dono da tabela (o papel de escrita da aplicação): RLS
-- ligado que não filtra nada. Falsa sensação de segurança.
--
-- Correção por tabela:
--   * Quatro têm tenancy real e ganham RLS de verdade (ENABLE + FORCE + policy fiel ao @PreAuthorize
--     de leitura), com escopo de ATOR via GUCs dedicadas app.pjb_actor_id / app.pjb_actor_roles
--     (independentes das GUCs de sigilo do processo, para não interferir no read model da V221).
--   * Três não têm coluna de dono/tenancy (referência de store de IA, fila de revisão de IA, balcão
--     por CPF/OAB em texto): RLS por usuário seria cosmético — o órfão é removido e a proteção real
--     segue na camada de aplicação (ABAC).
--
-- As policies são permissivas quando o contexto de ator está vazio (jobs, boot, migrations, anônimo),
-- e WITH CHECK (true) para não bloquear escrita — RLS aqui é backstop de LEITURA, defesa em profundidade
-- sobre o @PreAuthorize, não substituto dele.

-- Helpers de leitura das GUCs de ator.
CREATE OR REPLACE FUNCTION pjb_rls_actor_id() RETURNS text
LANGUAGE sql STABLE AS $$
    SELECT NULLIF(current_setting('app.pjb_actor_id', true), '')
$$;

CREATE OR REPLACE FUNCTION pjb_rls_has_role(role text) RETURNS boolean
LANGUAGE sql STABLE AS $$
    SELECT position('|' || role || '|' IN COALESCE(current_setting('app.pjb_actor_roles', true), '')) > 0
$$;

CREATE OR REPLACE FUNCTION pjb_rls_system_context() RETURNS boolean
LANGUAGE sql STABLE AS $$
    SELECT COALESCE(current_setting('app.pjb_actor_id', true), '') = ''
       AND COALESCE(current_setting('app.pjb_actor_roles', true), '') = ''
$$;

-- Sem tenancy: remover o órfão (app-layer é a proteção).
ALTER TABLE memory_store_ref           DISABLE ROW LEVEL SECURITY;
ALTER TABLE memory_candidate_review    DISABLE ROW LEVEL SECURITY;
ALTER TABLE balcao_virtual_atendimento DISABLE ROW LEVEL SECURITY;

-- support_ticket: dono (aberto_por), atendente (atendido_por), suporte técnico e admin.
-- Espelha GET /suporte/chamados/meus (dono) e GET /fila (ROLE_SUPORTE_TECNICO/ROLE_ADMINISTRADOR).
ALTER TABLE support_ticket FORCE ROW LEVEL SECURITY;
CREATE POLICY support_ticket_actor_scope ON support_ticket
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR pjb_rls_has_role('ROLE_SUPORTE_TECNICO')
        OR aberto_por_id::text = pjb_rls_actor_id()
        OR atendido_por_id::text = pjb_rls_actor_id()
    )
    WITH CHECK (true);

-- judge_travel_exception: o próprio juiz (usuario_id) e admin. Sem endpoint público; leitura interna
-- pela checagem de geofence do próprio ator, e provisionamento por admin/sistema.
ALTER TABLE judge_travel_exception FORCE ROW LEVEL SECURITY;
CREATE POLICY judge_travel_exception_actor_scope ON judge_travel_exception
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR usuario_id::text = pjb_rls_actor_id()
    )
    WITH CHECK (true);

-- legal_ai_audit_log: o próprio ator (ator_id) e admin. Escrita append-only pela infra de IA.
ALTER TABLE legal_ai_audit_log FORCE ROW LEVEL SECURITY;
CREATE POLICY legal_ai_audit_log_actor_scope ON legal_ai_audit_log
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR ator_id = pjb_rls_actor_id()
    )
    WITH CHECK (true);

-- intimacao_audiencia: leitura só por servidor judiciário e admin (espelha o @PreAuthorize do
-- IntimacaoAudienciaController). Comunicação processual — backstop de perfil sobre o app-layer.
ALTER TABLE intimacao_audiencia FORCE ROW LEVEL SECURITY;
CREATE POLICY intimacao_audiencia_reader_scope ON intimacao_audiencia
    USING (
        pjb_rls_system_context()
        OR pjb_rls_has_role('ROLE_ADMIN')
        OR pjb_rls_has_role('ROLE_ADMINISTRADOR')
        OR pjb_rls_has_role('ROLE_SERVIDOR_JUDICIARIO')
    )
    WITH CHECK (true);
