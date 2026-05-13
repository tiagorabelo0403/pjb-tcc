-- Julgamentos colegiados (2ª instância e superiores) + votos individualizados + acórdão.
-- Estrutura pensada para acompanhamento em tempo real via SSE e auditoria.

CREATE TABLE IF NOT EXISTS tb_julgamento_colegiado (
  id BIGSERIAL PRIMARY KEY,
  processo_id BIGINT NOT NULL,
  grau VARCHAR(30) NOT NULL,
  tribunal_sigla VARCHAR(40),
  orgao_julgador VARCHAR(120),
  relator_nome VARCHAR(120),
  revisor_nome VARCHAR(120),
  status VARCHAR(40) NOT NULL,
  pauta_data_hora TIMESTAMP,
  sessao_inicio TIMESTAMP,
  sessao_fim TIMESTAMP,
  placar_favor INT DEFAULT 0,
  placar_contra INT DEFAULT 0,
  placar_parcial INT DEFAULT 0,
  placar_outros INT DEFAULT 0,
  acordao_publicado BOOLEAN DEFAULT FALSE,
  acordao_publicado_em TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_julgamento_processo ON tb_julgamento_colegiado (processo_id);
CREATE INDEX IF NOT EXISTS idx_julgamento_grau_status ON tb_julgamento_colegiado (grau, status);

ALTER TABLE tb_julgamento_colegiado
  ADD CONSTRAINT fk_julgamento_processo
  FOREIGN KEY (processo_id) REFERENCES tb_processo(id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS tb_voto_colegiado (
  id BIGSERIAL PRIMARY KEY,
  julgamento_id BIGINT NOT NULL,
  ordem INT NOT NULL,
  magistrado_nome VARCHAR(140) NOT NULL,
  magistrado_cargo VARCHAR(60),
  papel VARCHAR(30),
  voto_tipo VARCHAR(60) NOT NULL,
  voto_resumo VARCHAR(800),
  proferido_em TIMESTAMP NOT NULL,
  documento_ref VARCHAR(300),
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_voto_julgamento ON tb_voto_colegiado (julgamento_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_voto_julgamento_ordem ON tb_voto_colegiado (julgamento_id, ordem);

ALTER TABLE tb_voto_colegiado
  ADD CONSTRAINT fk_voto_julgamento
  FOREIGN KEY (julgamento_id) REFERENCES tb_julgamento_colegiado(id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS tb_acordao (
  id BIGSERIAL PRIMARY KEY,
  julgamento_id BIGINT NOT NULL,
  numero_acordao VARCHAR(80),
  ementa_resumo TEXT,
  inteiro_teor_ref VARCHAR(300),
  publicado_em TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_acordao_julgamento ON tb_acordao (julgamento_id);
ALTER TABLE tb_acordao
  ADD CONSTRAINT fk_acordao_julgamento
  FOREIGN KEY (julgamento_id) REFERENCES tb_julgamento_colegiado(id) ON DELETE CASCADE;
