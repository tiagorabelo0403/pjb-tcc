CREATE TABLE IF NOT EXISTS tb_prontuario_nacional_entrada (
  id UUID PRIMARY KEY,
  identidade_id UUID NULL,
  documento VARCHAR(14) NOT NULL,
  documento_hash VARCHAR(64) NOT NULL,
  nome_sujeito VARCHAR(180) NOT NULL,
  nome_sujeito_chave VARCHAR(180) NOT NULL,
  nupn VARCHAR(50) NOT NULL,
  processo_local_id BIGINT NULL,
  tribunal_codigo VARCHAR(20) NOT NULL,
  tribunal_origem_uri VARCHAR(240) NULL,
  polo VARCHAR(20) NOT NULL,
  qualificacao VARCHAR(30) NOT NULL,
  ramo_direito VARCHAR(60) NULL,
  status_processo VARCHAR(60) NOT NULL,
  classe_processual VARCHAR(120) NULL,
  assunto VARCHAR(180) NULL,
  nivel_sigilo VARCHAR(40) NULL,
  origem_registro VARCHAR(30) NOT NULL,
  fonte_evento_id VARCHAR(80) NULL,
  ocorrido_em TIMESTAMPTZ NOT NULL,
  registrado_em TIMESTAMPTZ NOT NULL,
  atualizado_em TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_prontuario_nacional_natural
    UNIQUE (documento_hash, nupn, polo, qualificacao, tribunal_codigo),
  CONSTRAINT fk_prontuario_nacional_identidade
    FOREIGN KEY (identidade_id) REFERENCES tb_identidade_juridica_nacional (id) ON DELETE SET NULL,
  CONSTRAINT fk_prontuario_nacional_processo_local
    FOREIGN KEY (processo_local_id) REFERENCES tb_processo (id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS ix_prontuario_nacional_documento
  ON tb_prontuario_nacional_entrada (documento_hash, ocorrido_em DESC, atualizado_em DESC);

CREATE INDEX IF NOT EXISTS ix_prontuario_nacional_nupn
  ON tb_prontuario_nacional_entrada (nupn);

CREATE INDEX IF NOT EXISTS ix_prontuario_nacional_conflito
  ON tb_prontuario_nacional_entrada (documento_hash, polo, ramo_direito, status_processo);

CREATE INDEX IF NOT EXISTS ix_prontuario_nacional_identidade
  ON tb_prontuario_nacional_entrada (identidade_id);
