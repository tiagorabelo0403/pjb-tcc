CREATE TABLE support_ticket (
    id                          BIGSERIAL PRIMARY KEY,
    aberto_por_id               BIGINT NOT NULL REFERENCES tb_usuario(id),
    aberto_por_nome             VARCHAR(255) NOT NULL,
    aberto_por_tipo_usuario     VARCHAR(60),
    categoria                   VARCHAR(60) NOT NULL,
    assunto                     VARCHAR(255) NOT NULL,
    descricao                   TEXT NOT NULL,
    status                      VARCHAR(50) NOT NULL,
    atendido_por_id             BIGINT REFERENCES tb_usuario(id),
    atendido_por_nome           VARCHAR(255),
    resposta                    TEXT,
    viagem_uf_ou_pais_destino   VARCHAR(80),
    viagem_data_inicio          DATE,
    viagem_data_fim             DATE,
    viagem_justificativa        TEXT,
    criado_em                   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atendido_em                 TIMESTAMP WITH TIME ZONE,
    resolvido_em                TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_support_ticket_aberto_por ON support_ticket(aberto_por_id, status);
CREATE INDEX idx_support_ticket_fila       ON support_ticket(status, criado_em);
CREATE INDEX idx_support_ticket_categoria  ON support_ticket(categoria);

ALTER TABLE support_ticket ENABLE ROW LEVEL SECURITY;
