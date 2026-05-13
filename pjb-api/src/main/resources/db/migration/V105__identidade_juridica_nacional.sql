CREATE TABLE IF NOT EXISTS tb_identidade_juridica_nacional (
  id UUID PRIMARY KEY,
  tipo_documento VARCHAR(10) NOT NULL,
  documento VARCHAR(14) NOT NULL,
  documento_hash VARCHAR(64) NOT NULL,
  documento_formatado VARCHAR(20) NOT NULL,
  nome_canonico VARCHAR(180) NOT NULL,
  chave_pesquisa VARCHAR(180) NOT NULL,
  prontuario_nacional_uri VARCHAR(240) NOT NULL,
  receita_status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
  receita_verificado_em TIMESTAMPTZ NULL,
  receita_protocolo VARCHAR(120) NULL,
  oab_numero VARCHAR(20) NULL,
  oab_uf VARCHAR(2) NULL,
  oab_status VARCHAR(20) NOT NULL DEFAULT 'NAO_APLICAVEL',
  govbr_sub_hash VARCHAR(64) NULL,
  govbr_nivel VARCHAR(20) NOT NULL DEFAULT 'NAO_VINCULADO',
  govbr_vinculado_em TIMESTAMPTZ NULL,
  nivel_confianca VARCHAR(30) NOT NULL DEFAULT 'AUTODECLARADA',
  origem_cadastro VARCHAR(30) NOT NULL DEFAULT 'AUTOCADASTRO',
  bloqueio_unificacao BOOLEAN NOT NULL DEFAULT FALSE,
  versao_unificacao BIGINT NOT NULL DEFAULT 1,
  ultima_sincronizacao_em TIMESTAMPTZ NULL,
  criado_em TIMESTAMPTZ NOT NULL,
  atualizado_em TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_identidade_juridica_documento UNIQUE (tipo_documento, documento),
  CONSTRAINT uk_identidade_juridica_documento_hash UNIQUE (documento_hash)
);

CREATE INDEX IF NOT EXISTS ix_identidade_juridica_nome ON tb_identidade_juridica_nacional (chave_pesquisa);
CREATE INDEX IF NOT EXISTS ix_identidade_juridica_govbr ON tb_identidade_juridica_nacional (govbr_sub_hash);
CREATE INDEX IF NOT EXISTS ix_identidade_juridica_oab ON tb_identidade_juridica_nacional (oab_uf, oab_numero);

CREATE TABLE IF NOT EXISTS tb_identidade_juridica_papel (
  identidade_id UUID NOT NULL,
  papel VARCHAR(40) NOT NULL,
  CONSTRAINT pk_identidade_juridica_papel PRIMARY KEY (identidade_id, papel),
  CONSTRAINT fk_identidade_juridica_papel_identidade FOREIGN KEY (identidade_id)
    REFERENCES tb_identidade_juridica_nacional (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tb_identidade_juridica_alias (
  id BIGSERIAL PRIMARY KEY,
  identidade_id UUID NOT NULL,
  tipo_alias VARCHAR(30) NOT NULL,
  valor VARCHAR(180) NOT NULL,
  valor_normalizado VARCHAR(180) NOT NULL,
  criado_em TIMESTAMPTZ NOT NULL,
  CONSTRAINT uk_identidade_juridica_alias UNIQUE (identidade_id, tipo_alias, valor_normalizado),
  CONSTRAINT fk_identidade_juridica_alias_identidade FOREIGN KEY (identidade_id)
    REFERENCES tb_identidade_juridica_nacional (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_identidade_juridica_alias_lookup
  ON tb_identidade_juridica_alias (identidade_id, tipo_alias, valor_normalizado);

CREATE TABLE IF NOT EXISTS tb_identidade_juridica_auditoria (
  id BIGSERIAL PRIMARY KEY,
  identidade_id UUID NOT NULL,
  evento VARCHAR(50) NOT NULL,
  origem VARCHAR(40) NOT NULL,
  ator VARCHAR(120) NULL,
  descricao VARCHAR(300) NOT NULL,
  payload_hash VARCHAR(64) NULL,
  criado_em TIMESTAMPTZ NOT NULL,
  CONSTRAINT fk_identidade_juridica_auditoria_identidade FOREIGN KEY (identidade_id)
    REFERENCES tb_identidade_juridica_nacional (id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_identidade_juridica_auditoria_identidade
  ON tb_identidade_juridica_auditoria (identidade_id, criado_em DESC);

ALTER TABLE IF EXISTS tb_usuario
  ADD COLUMN IF NOT EXISTS identidade_juridica_id UUID NULL;

CREATE INDEX IF NOT EXISTS ix_usuario_identidade_juridica ON tb_usuario (identidade_juridica_id);
