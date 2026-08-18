CREATE TABLE IF NOT EXISTS tb_sessao_acordo_processual (
    id BIGSERIAL PRIMARY KEY,
    processo_id BIGINT NOT NULL,
    tipo_sala VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    aberta_por_id BIGINT NOT NULL,
    aberta_em TIMESTAMPTZ NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    motivo_abertura VARCHAR(1000) NOT NULL,
    segredo_justica BOOLEAN NOT NULL DEFAULT FALSE,
    confidencialidade_nivel VARCHAR(40) NOT NULL,
    cejusc_referenciado BOOLEAN NOT NULL DEFAULT FALSE,
    homologado_em TIMESTAMPTZ,
    homologado_por_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sap_processo FOREIGN KEY (processo_id) REFERENCES tb_processo(id),
    CONSTRAINT fk_sap_aberta_por FOREIGN KEY (aberta_por_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_sap_homologado_por FOREIGN KEY (homologado_por_id) REFERENCES tb_usuario(id),
    CONSTRAINT ck_sap_tipo CHECK (tipo_sala IN ('PROCESSUAL_CONTROLADA','CONCILIACAO','MEDIACAO','CEJUSC','RECURSAL','EXECUCAO')),
    CONSTRAINT ck_sap_status CHECK (status IN ('NOT_ELIGIBLE','ELIGIBLE','INVITED','WAITING_PARTICIPANTS','OPEN','PAUSED','PROPOSAL_PENDING','COUNTERPROPOSAL_PENDING','AGREEMENT_DRAFTED','WAITING_SIGNATURES','SIGNED','SENT_TO_HOMOLOGATION','HOMOLOGATED','REJECTED_BY_JUDGE','FAILED','EXPIRED','CLOSED')),
    CONSTRAINT ck_sap_conf CHECK (confidencialidade_nivel IN ('PUBLICA_CONTROLADA','RESTRITA_A_PARTICIPANTES','SIGILOSA','SEGREDO_JUSTICA')),
    CONSTRAINT ck_sap_expira CHECK (expira_em > aberta_em)
);

CREATE INDEX IF NOT EXISTS idx_sap_processo ON tb_sessao_acordo_processual (processo_id);
CREATE INDEX IF NOT EXISTS idx_sap_status ON tb_sessao_acordo_processual (status);
CREATE INDEX IF NOT EXISTS idx_sap_expira ON tb_sessao_acordo_processual (expira_em);
CREATE INDEX IF NOT EXISTS idx_sap_status_expira ON tb_sessao_acordo_processual (status, expira_em);

CREATE TABLE IF NOT EXISTS tb_acordo_participante (
    id BIGSERIAL PRIMARY KEY,
    sessao_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    papel VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    aceitou_em TIMESTAMPTZ,
    recusou_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_acp_sessao FOREIGN KEY (sessao_id) REFERENCES tb_sessao_acordo_processual(id) ON DELETE CASCADE,
    CONSTRAINT fk_acp_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id),
    CONSTRAINT uk_acordo_part_sessao_usuario UNIQUE (sessao_id, usuario_id),
    CONSTRAINT ck_acp_papel CHECK (papel IN ('PARTE','ADVOGADO','CONCILIADOR','MEDIADOR','MAGISTRADO','SERVIDOR_AUTORIZADO')),
    CONSTRAINT ck_acp_status CHECK (status IN ('CONVIDADO','ACEITO','RECUSADO','REMOVIDO'))
);

CREATE INDEX IF NOT EXISTS idx_acordo_part_sessao ON tb_acordo_participante (sessao_id);
CREATE INDEX IF NOT EXISTS idx_acordo_part_sessao_status ON tb_acordo_participante (sessao_id, status);
CREATE INDEX IF NOT EXISTS idx_acordo_part_usuario ON tb_acordo_participante (usuario_id);

CREATE TABLE IF NOT EXISTS tb_acordo_mensagem (
    id BIGSERIAL PRIMARY KEY,
    sessao_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    conteudo TEXT NOT NULL,
    confidencial BOOLEAN NOT NULL DEFAULT FALSE,
    visibilidade VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_acm_sessao FOREIGN KEY (sessao_id) REFERENCES tb_sessao_acordo_processual(id) ON DELETE CASCADE,
    CONSTRAINT fk_acm_autor FOREIGN KEY (autor_id) REFERENCES tb_usuario(id),
    CONSTRAINT ck_acm_tipo CHECK (tipo IN ('TEXTO','SISTEMA','DOCUMENTO','AUDIENCIA','ESCLARECIMENTO')),
    CONSTRAINT ck_acm_visibilidade CHECK (visibilidade IN ('PARTICIPANTES','CONFIDENCIAL','MAGISTRADO','PUBLICA_PROCESSUAL')),
    CONSTRAINT ck_acm_conf_publica CHECK (NOT (confidencial = TRUE AND visibilidade = 'PUBLICA_PROCESSUAL'))
);

CREATE INDEX IF NOT EXISTS idx_acordo_msg_sessao ON tb_acordo_mensagem (sessao_id);
CREATE INDEX IF NOT EXISTS idx_acordo_msg_sessao_created ON tb_acordo_mensagem (sessao_id, created_at);
CREATE INDEX IF NOT EXISTS idx_acordo_msg_autor ON tb_acordo_mensagem (autor_id);

CREATE TABLE IF NOT EXISTS tb_acordo_proposta (
    id BIGSERIAL PRIMARY KEY,
    sessao_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    valor NUMERIC(19,2),
    termos_json JSONB NOT NULL,
    validade_ate TIMESTAMPTZ NOT NULL,
    status VARCHAR(40) NOT NULL,
    criada_por_ia BOOLEAN NOT NULL DEFAULT FALSE,
    revisada_por_humano BOOLEAN NOT NULL DEFAULT FALSE,
    revisada_por_id BIGINT,
    revisada_em TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_acpr_sessao FOREIGN KEY (sessao_id) REFERENCES tb_sessao_acordo_processual(id) ON DELETE CASCADE,
    CONSTRAINT fk_acpr_autor FOREIGN KEY (autor_id) REFERENCES tb_usuario(id),
    CONSTRAINT fk_acpr_revisor FOREIGN KEY (revisada_por_id) REFERENCES tb_usuario(id),
    CONSTRAINT ck_acpr_tipo CHECK (tipo IN ('FORMAL','CONTRAPROPOSTA')),
    CONSTRAINT ck_acpr_status CHECK (status IN ('PENDENTE','AGUARDANDO_REVISAO_HUMANA','ACEITA','REJEITADA','EXPIRADA','SUBSTITUIDA')),
    CONSTRAINT ck_acpr_valor CHECK (valor IS NULL OR valor >= 0),
    CONSTRAINT ck_acpr_ia_review CHECK (criada_por_ia = FALSE OR revisada_por_humano = TRUE OR status = 'AGUARDANDO_REVISAO_HUMANA')
);

CREATE INDEX IF NOT EXISTS idx_acordo_prop_sessao ON tb_acordo_proposta (sessao_id);
CREATE INDEX IF NOT EXISTS idx_acordo_prop_sessao_status ON tb_acordo_proposta (sessao_id, status);
CREATE INDEX IF NOT EXISTS idx_acordo_prop_validade ON tb_acordo_proposta (validade_ate);
CREATE INDEX IF NOT EXISTS idx_acordo_prop_autor ON tb_acordo_proposta (autor_id);

CREATE TABLE IF NOT EXISTS tb_acordo_termo (
    id BIGSERIAL PRIMARY KEY,
    sessao_id BIGINT NOT NULL,
    proposta_id BIGINT NOT NULL,
    conteudo_termo TEXT NOT NULL,
    hash_termo VARCHAR(64) NOT NULL,
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_act_sessao FOREIGN KEY (sessao_id) REFERENCES tb_sessao_acordo_processual(id) ON DELETE CASCADE,
    CONSTRAINT fk_act_proposta FOREIGN KEY (proposta_id) REFERENCES tb_acordo_proposta(id),
    CONSTRAINT uk_act_proposta UNIQUE (proposta_id),
    CONSTRAINT ck_act_status CHECK (status IN ('MINUTA','AGUARDANDO_ASSINATURAS','ASSINADO','ENVIADO_HOMOLOGACAO','HOMOLOGADO','REJEITADO')),
    CONSTRAINT ck_act_hash CHECK (char_length(hash_termo) = 64)
);

CREATE INDEX IF NOT EXISTS idx_acordo_termo_sessao ON tb_acordo_termo (sessao_id);
CREATE INDEX IF NOT EXISTS idx_acordo_termo_proposta ON tb_acordo_termo (proposta_id);
CREATE INDEX IF NOT EXISTS idx_acordo_termo_status ON tb_acordo_termo (status);

CREATE TABLE IF NOT EXISTS tb_acordo_auditoria (
    id BIGSERIAL PRIMARY KEY,
    sessao_id BIGINT NOT NULL,
    usuario_id BIGINT,
    evento VARCHAR(40) NOT NULL,
    detalhes_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    ip_hash VARCHAR(64),
    user_agent_hash VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_aca_sessao FOREIGN KEY (sessao_id) REFERENCES tb_sessao_acordo_processual(id) ON DELETE CASCADE,
    CONSTRAINT fk_aca_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id),
    CONSTRAINT ck_aca_evento CHECK (evento IN ('ABERTURA','CONVITE','ACEITE','RECUSA','MENSAGEM','PROPOSTA','CONTRAPROPOSTA','GERACAO_TERMO','REVISAO_HUMANA','ASSINATURA','ENVIO_HOMOLOGACAO','HOMOLOGACAO','REJEICAO','ENCERRAMENTO','EXPIRACAO')),
    CONSTRAINT ck_aca_ip_hash CHECK (ip_hash IS NULL OR char_length(ip_hash) = 64),
    CONSTRAINT ck_aca_ua_hash CHECK (user_agent_hash IS NULL OR char_length(user_agent_hash) = 64)
);

CREATE INDEX IF NOT EXISTS idx_acordo_audit_sessao ON tb_acordo_auditoria (sessao_id);
CREATE INDEX IF NOT EXISTS idx_acordo_audit_sessao_created ON tb_acordo_auditoria (sessao_id, created_at);
CREATE INDEX IF NOT EXISTS idx_acordo_audit_usuario ON tb_acordo_auditoria (usuario_id, created_at);
CREATE INDEX IF NOT EXISTS idx_acordo_audit_evento ON tb_acordo_auditoria (evento, created_at);
