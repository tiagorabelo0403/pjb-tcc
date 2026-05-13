-- LAIANE Lote 2: procurações, teses e auditoria reforçada (PostgreSQL)

--
-- Este script cria as tabelas necessárias para o Lote 2 do módulo Laiane
-- utilizando sintaxe compatível com PostgreSQL. A versão anterior usava
-- instruções específicas do MySQL (AUTO_INCREMENT, DATETIME) e não
-- correspondia ao modelo de entidades Java. Aqui definimos tipos e
-- restrições que refletem as classes JPA existentes, incluindo o
-- armazenamento de UUID nativo para auditoria.

-- =============================================================================
-- Tabela de auditoria de eventos
--
-- Registra ações executadas no sistema com rastreabilidade de usuário,
-- referência e justificativa. Permite auditoria reforçada, conforme as
-- especificações do Lote 2 (incluindo hash de integridade e perfil
-- comportamental). O campo nivel_risco é numérico para permitir cálculos e
-- ordenações sem casting.
-- =============================================================================
CREATE TABLE IF NOT EXISTS auditoria_eventos (
    id              BIGSERIAL PRIMARY KEY,
    uuid            UUID NOT NULL UNIQUE,
    acao            VARCHAR(120) NOT NULL,
    usuario_id      BIGINT NOT NULL,
    referencia_id   VARCHAR(120),
    detalhes        TEXT,
    justificativa   TEXT,
    hash_integridade VARCHAR(128),
    timestamp       TIMESTAMP NOT NULL,
    nivel_risco     DOUBLE PRECISION NOT NULL DEFAULT 0,
    perfil_comportamental VARCHAR(80)
);

CREATE INDEX IF NOT EXISTS idx_auditoria_usuario_ts ON auditoria_eventos(usuario_id, timestamp);
CREATE INDEX IF NOT EXISTS idx_auditoria_ref_ts ON auditoria_eventos(referencia_id, timestamp);

-- =============================================================================
-- Procurações (tb_laiane_procuracao)
--
-- Contém dados de procurações cadastradas por advogados para representar
-- clientes em processos. Referencia o usuário advogado via chave
-- estrangeira. Campos de data são do tipo DATE, compatíveis com LocalDate.
-- =============================================================================
CREATE TABLE IF NOT EXISTS tb_laiane_procuracao (
    id              BIGSERIAL PRIMARY KEY,
    advogado_id     BIGINT NOT NULL,
    cliente_id      BIGINT,
    processo_id     BIGINT,
    status          VARCHAR(32) NOT NULL,
    inicio_vigencia DATE,
    fim_vigencia    DATE,
    poderes         TEXT,
    anexos_json     TEXT,
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP,
    CONSTRAINT fk_laiane_procuracao_adv FOREIGN KEY (advogado_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_laiane_procuracao_adv ON tb_laiane_procuracao(advogado_id);

-- =============================================================================
-- Teses (tb_laiane_tese)
--
-- Guarda as teses jurídicas cadastradas por advogados. O campo corpo permite
-- armazenar textos longos. Tags são armazenadas em formato JSON textual.
-- =============================================================================
CREATE TABLE IF NOT EXISTS tb_laiane_tese (
    id          BIGSERIAL PRIMARY KEY,
    advogado_id BIGINT NOT NULL,
    area        VARCHAR(64),
    titulo      VARCHAR(255) NOT NULL,
    corpo       TEXT NOT NULL,
    tags_json   TEXT,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT fk_laiane_tese_adv FOREIGN KEY (advogado_id) REFERENCES tb_usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_laiane_tese_adv ON tb_laiane_tese(advogado_id);