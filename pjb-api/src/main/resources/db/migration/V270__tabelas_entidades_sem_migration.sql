-- estados
CREATE TABLE IF NOT EXISTS estados (
    uf    VARCHAR(2)   NOT NULL PRIMARY KEY,
    nome  VARCHAR(100) NOT NULL UNIQUE,
    ativo BOOLEAN      NOT NULL DEFAULT true
);

-- municipios
CREATE TABLE IF NOT EXISTS municipios (
    ibge_code BIGINT       NOT NULL PRIMARY KEY,
    nome      VARCHAR(200) NOT NULL,
    uf        VARCHAR(2)   NOT NULL
);

-- metadados_sistema
CREATE TABLE IF NOT EXISTS metadados_sistema (
    id             BIGSERIAL    PRIMARY KEY,
    chave          VARCHAR(255) NOT NULL UNIQUE,
    valor          TEXT         NOT NULL,
    fonte          VARCHAR(255),
    versao         VARCHAR(50),
    data_registro  TIMESTAMP    NOT NULL
);

-- modelos_contrato
CREATE TABLE IF NOT EXISTS modelos_contrato (
    id              BIGSERIAL      PRIMARY KEY,
    nome            VARCHAR(100)   NOT NULL,
    materia         VARCHAR(30)    NOT NULL,
    template_html   TEXT           NOT NULL,
    fase_processual VARCHAR(30),
    valor_maximo    NUMERIC(19, 2),
    ativo           BOOLEAN        NOT NULL DEFAULT true
);

-- ia_profiles
CREATE TABLE IF NOT EXISTS ia_profiles (
    id          BIGSERIAL   PRIMARY KEY,
    ramo        VARCHAR(60),
    especie     VARCHAR(60),
    competencia VARCHAR(60)
);
