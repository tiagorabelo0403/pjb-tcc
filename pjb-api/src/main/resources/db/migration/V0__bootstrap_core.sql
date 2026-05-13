-- ==========================================================
-- V0 - Bootstrap mínimo para rodar o projeto do zero (PostgreSQL)
--
-- Objetivo:
--  1) Criar tabelas base referenciadas pelas migrations V1..V4
--  2) Permitir que um banco novo seja criado e migrado sem falhar
--
-- Observação:
--  Este bootstrap é "mínimo funcional" para o fluxo core + Laiane.
--  Caso o projeto contenha outras entidades que ainda não possuam migration
--  dedicada, recomenda-se complementar este script em versões futuras.
-- ==========================================================

-- Usuários
CREATE TABLE IF NOT EXISTS tb_usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    perfil VARCHAR(60) NOT NULL,
    data_cadastro TIMESTAMP,
    data_atualizacao TIMESTAMP,
    senha TEXT NOT NULL,
    cpf VARCHAR(11) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    comarca VARCHAR(120),
    uf VARCHAR(2),
    tipo_usuario VARCHAR(60) NOT NULL,
    ente_federativo VARCHAR(40),
    papel_equipe VARCHAR(40),
    registro_profissional VARCHAR(60)
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tb_usuario_email') THEN
        ALTER TABLE tb_usuario ADD CONSTRAINT uk_tb_usuario_email UNIQUE (email);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_tb_usuario_cpf') THEN
        ALTER TABLE tb_usuario ADD CONSTRAINT uk_tb_usuario_cpf UNIQUE (cpf);
    END IF;
END $$;

-- Especialidades (mínimo para relacionamento)
CREATE TABLE IF NOT EXISTS tb_especialidade_pericial (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    area VARCHAR(120),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS tb_usuario_especialidade (
    usuario_id BIGINT NOT NULL,
    especialidade_id BIGINT NOT NULL
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_usuario_especialidade_usuario') THEN
        ALTER TABLE tb_usuario_especialidade
            ADD CONSTRAINT fk_usuario_especialidade_usuario
            FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_usuario_especialidade_especialidade') THEN
        ALTER TABLE tb_usuario_especialidade
            ADD CONSTRAINT fk_usuario_especialidade_especialidade
            FOREIGN KEY (especialidade_id) REFERENCES tb_especialidade_pericial(id);
    END IF;
END $$;

-- Jurisdições (mínimo para Processo + filtros)
CREATE TABLE IF NOT EXISTS jurisdicoes (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30),
    sigla VARCHAR(12),
    nome VARCHAR(120) NOT NULL,
    tipo VARCHAR(30),
    natureza VARCHAR(30),
    grau VARCHAR(30),
    esfera VARCHAR(30),
    materia VARCHAR(40),
    competencia_material VARCHAR(120),
    competencia_territorial VARCHAR(120),
    comarca VARCHAR(120),
    estado VARCHAR(2),
    regiao VARCHAR(30),
    teto_valor_causa NUMERIC(17,2),
    valor_minimo_causa NUMERIC(17,2),
    recurso_para_id BIGINT,
    prazo_comum_dias INT,
    prazo_recursos_dias INT,
    prazo_resposta_dias INT,
    permite_juizado_especial BOOLEAN,
    permite_tutela_antecipada BOOLEAN,
    exige_audiencia_conciliacao BOOLEAN,
    permite_acao_coletiva BOOLEAN,
    ativo BOOLEAN,
    versao BIGINT DEFAULT 0,
    data_criacao TIMESTAMP,
    data_atualizacao TIMESTAMP
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_jurisdicao_recurso_para') THEN
        ALTER TABLE jurisdicoes
            ADD CONSTRAINT fk_jurisdicao_recurso_para
            FOREIGN KEY (recurso_para_id) REFERENCES jurisdicoes(id);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS jurisdicao_regras (
    jurisdicao_id BIGINT NOT NULL,
    regra VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS jurisdicao_normas (
    jurisdicao_id BIGINT NOT NULL,
    norma VARCHAR(255) NOT NULL
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_jurisdicao_regras') THEN
        ALTER TABLE jurisdicao_regras
            ADD CONSTRAINT fk_jurisdicao_regras
            FOREIGN KEY (jurisdicao_id) REFERENCES jurisdicoes(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_jurisdicao_normas') THEN
        ALTER TABLE jurisdicao_normas
            ADD CONSTRAINT fk_jurisdicao_normas
            FOREIGN KEY (jurisdicao_id) REFERENCES jurisdicoes(id);
    END IF;
END $$;

-- Processos (mínimo para V1 e módulos)
CREATE TABLE IF NOT EXISTS tb_processo (
    id BIGSERIAL PRIMARY KEY,
    numero_unificado VARCHAR(40),
    numero_processo VARCHAR(40),
    tipo_justica VARCHAR(40),
    ramo_direito VARCHAR(60),
    materia VARCHAR(60),
    classe_processual VARCHAR(120),
    assunto VARCHAR(255),
    modulo VARCHAR(40),
    jurisdicao_id BIGINT,
    usuario_id BIGINT,
    status_processo VARCHAR(40),
    fase_atual VARCHAR(40),
    rito_processual VARCHAR(80),
    nivel_sigilo VARCHAR(40),
    valor_causa NUMERIC(19,2),
    parte_autora_nome VARCHAR(255),
    parte_reu_nome VARCHAR(255),
    resultado_final VARCHAR(255),
    resumo_ia TEXT,
    score_complexidade INT,
    data_distribuicao TIMESTAMP,
    data_criacao TIMESTAMP,
    data_ultima_movimentacao TIMESTAMP
);

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_processo_usuario') THEN
        ALTER TABLE tb_processo ADD CONSTRAINT fk_processo_usuario FOREIGN KEY (usuario_id) REFERENCES tb_usuario(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_processo_jurisdicao') THEN
        ALTER TABLE tb_processo ADD CONSTRAINT fk_processo_jurisdicao FOREIGN KEY (jurisdicao_id) REFERENCES jurisdicoes(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_processo_numero_unificado ON tb_processo(numero_unificado);
CREATE INDEX IF NOT EXISTS idx_processo_usuario_id ON tb_processo(usuario_id);
