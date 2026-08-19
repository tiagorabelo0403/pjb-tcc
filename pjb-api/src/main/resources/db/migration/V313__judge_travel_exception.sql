CREATE TABLE judge_travel_exception (
    id                  BIGSERIAL PRIMARY KEY,
    usuario_id          BIGINT NOT NULL REFERENCES tb_usuario(id),
    uf_ou_pais_destino  VARCHAR(80) NOT NULL,
    data_inicio         DATE NOT NULL,
    data_fim            DATE NOT NULL,
    ticket_origem_id    BIGINT,
    criado_em           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_judge_travel_exception_usuario ON judge_travel_exception(usuario_id, data_inicio, data_fim);

ALTER TABLE judge_travel_exception ENABLE ROW LEVEL SECURITY;
