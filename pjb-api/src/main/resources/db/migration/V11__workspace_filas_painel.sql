-- Workspace: Filas (painel de pendências por perfil)
-- PJB 2026 (Java 21 / Spring Boot 3.x)

CREATE TABLE IF NOT EXISTS tb_workspace_fila (
    id UUID PRIMARY KEY,
    sistema BOOLEAN NOT NULL DEFAULT FALSE,
    audience VARCHAR(40) NOT NULL DEFAULT 'ALL',
    owner_user_id BIGINT,
    nome VARCHAR(120) NOT NULL,
    descricao VARCHAR(400),
    kind VARCHAR(30) NOT NULL,            -- WORKITEM | PROCESSO
    criterio_json TEXT NOT NULL,
    order_index INTEGER NOT NULL DEFAULT 0,
    compartilhado BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em TIMESTAMP,
    atualizado_em TIMESTAMP
);

-- Índices (para painel rápido)
CREATE INDEX IF NOT EXISTS idx_workspace_fila_owner ON tb_workspace_fila(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_workspace_fila_sistema ON tb_workspace_fila(sistema);
CREATE INDEX IF NOT EXISTS idx_workspace_fila_audience ON tb_workspace_fila(audience);

-- Uniqueness (case-insensitive):
--  - por usuário: owner_user_id + lower(nome)
--  - por sistema: audience + lower(nome)
CREATE UNIQUE INDEX IF NOT EXISTS ux_workspace_fila_owner_nome_ci
    ON tb_workspace_fila(owner_user_id, lower(nome))
    WHERE owner_user_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_workspace_fila_sistema_audience_nome_ci
    ON tb_workspace_fila(audience, lower(nome))
    WHERE sistema = TRUE;

-- Seed de filas do sistema (IDs determinísticos)
-- Observação: owner_user_id = NULL indica fila global (aplicável conforme audience)

-- ALL: Inbox (tarefas acessíveis)
INSERT INTO tb_workspace_fila (id, sistema, audience, owner_user_id, nome, descricao, kind, criterio_json, order_index, compartilhado, criado_em, atualizado_em)
SELECT '00000000-0000-0000-0000-000000000101'::uuid, TRUE, 'ALL', NULL,
       'Inbox (tarefas)',
       'Pendências geradas pelo motor de ritos (prioriza tarefas atribuídas a você e, na ausência, pool do seu perfil/território).',
       'WORKITEM', '{"mode":"AUTO_INBOX"}',
       10, FALSE, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM tb_workspace_fila WHERE sistema = TRUE AND audience = 'ALL' AND lower(nome) = lower('Inbox (tarefas)')
);

-- ALL: Urgentes 48h
INSERT INTO tb_workspace_fila (id, sistema, audience, owner_user_id, nome, descricao, kind, criterio_json, order_index, compartilhado, criado_em, atualizado_em)
SELECT '00000000-0000-0000-0000-000000000102'::uuid, TRUE, 'ALL', NULL,
       'Urgentes (48h)',
       'Tarefas com prazo nas próximas 48 horas (inclui atrasadas).',
       'WORKITEM', '{"mode":"DUE_WITHIN_HOURS","hours":48,"includeOverdue":true}',
       20, FALSE, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM tb_workspace_fila WHERE sistema = TRUE AND audience = 'ALL' AND lower(nome) = lower('Urgentes (48h)')
);

-- MAGISTRATURA: Minutas / decisões
INSERT INTO tb_workspace_fila (id, sistema, audience, owner_user_id, nome, descricao, kind, criterio_json, order_index, compartilhado, criado_em, atualizado_em)
SELECT '00000000-0000-0000-0000-000000000103'::uuid, TRUE, 'MAGISTRATURA', NULL,
       'Minutas (despachos/decisões)',
       'Fila focada em despachos e decisões pendentes no seu escopo.',
       'WORKITEM', '{"mode":"AUTO_INBOX","types":["DESPACHO","DECISAO"],"blockingOnly":false}',
       30, FALSE, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM tb_workspace_fila WHERE sistema = TRUE AND audience = 'MAGISTRATURA' AND lower(nome) = lower('Minutas (despachos/decisões)')
);

-- MAGISTRATURA: Blocking / travas
INSERT INTO tb_workspace_fila (id, sistema, audience, owner_user_id, nome, descricao, kind, criterio_json, order_index, compartilhado, criado_em, atualizado_em)
SELECT '00000000-0000-0000-0000-000000000104'::uuid, TRUE, 'MAGISTRATURA', NULL,
       'Travas (blocking)',
       'Tarefas marcadas como blocking (impedem avanço de fase enquanto não concluídas).',
       'WORKITEM', '{"mode":"AUTO_INBOX","blockingOnly":true}',
       40, FALSE, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM tb_workspace_fila WHERE sistema = TRUE AND audience = 'MAGISTRATURA' AND lower(nome) = lower('Travas (blocking)')
);

-- SERVIDOR: Expedições
INSERT INTO tb_workspace_fila (id, sistema, audience, owner_user_id, nome, descricao, kind, criterio_json, order_index, compartilhado, criado_em, atualizado_em)
SELECT '00000000-0000-0000-0000-000000000105'::uuid, TRUE, 'SERVIDOR', NULL,
       'Expedições / Comunicações',
       'Fila de expedição/intimação/citação no seu escopo.',
       'WORKITEM', '{"mode":"AUTO_INBOX","types":["EXPEDICAO","INTIMACAO","CITACAO","JUNTADA"]}',
       30, FALSE, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM tb_workspace_fila WHERE sistema = TRUE AND audience = 'SERVIDOR' AND lower(nome) = lower('Expedições / Comunicações')
);

-- MP: Manifestações
INSERT INTO tb_workspace_fila (id, sistema, audience, owner_user_id, nome, descricao, kind, criterio_json, order_index, compartilhado, criado_em, atualizado_em)
SELECT '00000000-0000-0000-0000-000000000106'::uuid, TRUE, 'MP', NULL,
       'Manifestações',
       'Fila de manifestações pendentes no escopo do Ministério Público.',
       'WORKITEM', '{"mode":"AUTO_INBOX","types":["MANIFESTACAO"]}',
       30, FALSE, now(), now()
WHERE NOT EXISTS (
    SELECT 1 FROM tb_workspace_fila WHERE sistema = TRUE AND audience = 'MP' AND lower(nome) = lower('Manifestações')
);
