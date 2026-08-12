CREATE TABLE secretaria_institucional_item (
    id                          BIGSERIAL PRIMARY KEY,
    processo_id                 BIGINT NOT NULL REFERENCES tb_processo(id),
    unidade_institucional_id    BIGINT REFERENCES tb_unidade_institucional(id),
    tipo_instituicao_alvo       VARCHAR(60) NOT NULL,
    motivo                      VARCHAR(20) NOT NULL,
    status                      VARCHAR(25) NOT NULL,
    prazo_base_dias             INTEGER NOT NULL,
    prazo_em_dobro              BOOLEAN NOT NULL,
    intimado_em                 TIMESTAMP WITH TIME ZONE,
    intimacao_tacita_em         TIMESTAMP WITH TIME ZONE,
    prazo_fatal                 TIMESTAMP WITH TIME ZONE,
    criado_em                   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_secretaria_inst_item_processo ON secretaria_institucional_item(processo_id, tipo_instituicao_alvo);
CREATE INDEX idx_secretaria_inst_item_unidade ON secretaria_institucional_item(unidade_institucional_id, prazo_fatal);
CREATE INDEX idx_secretaria_inst_item_status_ciencia ON secretaria_institucional_item(status, intimado_em, criado_em);

CREATE UNIQUE INDEX uq_secretaria_inst_item_ativo_por_processo_tipo
    ON secretaria_institucional_item(processo_id, tipo_instituicao_alvo)
    WHERE status IN ('PENDENTE', 'EM_ANALISE');
