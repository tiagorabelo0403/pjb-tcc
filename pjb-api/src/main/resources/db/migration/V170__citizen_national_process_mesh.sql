CREATE TABLE IF NOT EXISTS tb_processo_vinculo_nacional (
  id UUID PRIMARY KEY,
  identidade_id UUID NOT NULL,
  documento_hash VARCHAR(64) NOT NULL,
  nupn VARCHAR(50) NOT NULL,
  processo_local_id BIGINT NULL,
  tribunal_codigo VARCHAR(20) NOT NULL,
  tribunal_origem_uri VARCHAR(240) NULL,
  sistema_origem VARCHAR(20) NULL,
  papel_processual VARCHAR(40) NOT NULL,
  polo_processual VARCHAR(20) NOT NULL,
  qualificacao_original VARCHAR(40) NOT NULL,
  ramo_direito VARCHAR(60) NULL,
  status_processo VARCHAR(60) NOT NULL,
  classe_processual VARCHAR(160) NULL,
  assunto VARCHAR(240) NULL,
  nivel_sigilo VARCHAR(40) NULL,
  grau_confianca VARCHAR(40) NOT NULL,
  score_confianca INTEGER NOT NULL,
  origem_vinculo VARCHAR(40) NOT NULL,
  visivel_painel_pessoal BOOLEAN NOT NULL DEFAULT TRUE,
  exige_step_up BOOLEAN NOT NULL DEFAULT FALSE,
  contestado BOOLEAN NOT NULL DEFAULT FALSE,
  ocorrido_em TIMESTAMPTZ NOT NULL,
  criado_em TIMESTAMPTZ NOT NULL,
  atualizado_em TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_processo_vinculo_nacional UNIQUE (identidade_id, nupn, papel_processual),
  CONSTRAINT fk_processo_vinculo_nacional_identidade FOREIGN KEY (identidade_id)
    REFERENCES tb_identidade_juridica_nacional (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_processo_vinculo_nacional_identidade
  ON tb_processo_vinculo_nacional (identidade_id, atualizado_em DESC);
CREATE INDEX IF NOT EXISTS ix_processo_vinculo_nacional_doc
  ON tb_processo_vinculo_nacional (documento_hash, atualizado_em DESC);
CREATE INDEX IF NOT EXISTS ix_processo_vinculo_nacional_nupn
  ON tb_processo_vinculo_nacional (nupn);
CREATE INDEX IF NOT EXISTS ix_processo_vinculo_nacional_proc_local
  ON tb_processo_vinculo_nacional (processo_local_id);

CREATE TABLE IF NOT EXISTS tb_processo_visibilidade_pessoal (
  id UUID PRIMARY KEY,
  nupn VARCHAR(50) NOT NULL,
  processo_local_id BIGINT NULL,
  escopo VARCHAR(40) NOT NULL,
  visivel BOOLEAN NOT NULL,
  fundamento VARCHAR(500) NULL,
  concedido_por_usuario_id BIGINT NULL,
  concedido_por_perfil VARCHAR(60) NULL,
  expira_em TIMESTAMPTZ NULL,
  criado_em TIMESTAMPTZ NOT NULL,
  atualizado_em TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_processo_visibilidade_pessoal UNIQUE (nupn, escopo)
);

CREATE INDEX IF NOT EXISTS ix_processo_visibilidade_pessoal_proc_local
  ON tb_processo_visibilidade_pessoal (processo_local_id);
CREATE INDEX IF NOT EXISTS ix_processo_visibilidade_pessoal_visivel
  ON tb_processo_visibilidade_pessoal (escopo, visivel, atualizado_em DESC);

CREATE TABLE IF NOT EXISTS tb_cidadao_processo_nacional_projection (
  id UUID PRIMARY KEY,
  identidade_id UUID NOT NULL,
  documento_hash VARCHAR(64) NOT NULL,
  nupn VARCHAR(50) NOT NULL,
  processo_local_id BIGINT NULL,
  numero_exibicao VARCHAR(50) NOT NULL,
  tribunal_codigo VARCHAR(20) NOT NULL,
  sistema_origem VARCHAR(20) NULL,
  uf VARCHAR(2) NULL,
  comarca VARCHAR(120) NULL,
  unidade_judicial VARCHAR(180) NULL,
  papel_processual VARCHAR(40) NOT NULL,
  grau_confianca VARCHAR(40) NOT NULL,
  score_confianca INTEGER NOT NULL,
  origem_vinculo VARCHAR(40) NOT NULL,
  status_processo VARCHAR(60) NULL,
  fase_atual VARCHAR(60) NULL,
  ramo_direito VARCHAR(60) NULL,
  classe_processual VARCHAR(160) NULL,
  assunto VARCHAR(240) NULL,
  nivel_sigilo VARCHAR(40) NULL,
  data_distribuicao TIMESTAMPTZ NULL,
  data_ultima_movimentacao TIMESTAMPTZ NULL,
  ultima_movimentacao_resumo VARCHAR(240) NULL,
  arquivado BOOLEAN NOT NULL DEFAULT FALSE,
  oculto_por_politica_arquivo BOOLEAN NOT NULL DEFAULT FALSE,
  reexposto_secretaria BOOLEAN NOT NULL DEFAULT FALSE,
  visivel_painel_pessoal BOOLEAN NOT NULL DEFAULT TRUE,
  exige_step_up BOOLEAN NOT NULL DEFAULT FALSE,
  origem_externa_uri VARCHAR(240) NULL,
  sort_key BIGINT NOT NULL DEFAULT 0,
  gerado_em TIMESTAMPTZ NOT NULL,
  atualizado_em TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_cidadao_processo_projection UNIQUE (identidade_id, nupn),
  CONSTRAINT fk_cidadao_processo_projection_identidade FOREIGN KEY (identidade_id)
    REFERENCES tb_identidade_juridica_nacional (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_cidadao_processo_projection_identidade_sort
  ON tb_cidadao_processo_nacional_projection (identidade_id, visivel_painel_pessoal, sort_key DESC);
CREATE INDEX IF NOT EXISTS ix_cidadao_processo_projection_doc
  ON tb_cidadao_processo_nacional_projection (documento_hash, sort_key DESC);
CREATE INDEX IF NOT EXISTS ix_cidadao_processo_projection_proc_local
  ON tb_cidadao_processo_nacional_projection (processo_local_id);
