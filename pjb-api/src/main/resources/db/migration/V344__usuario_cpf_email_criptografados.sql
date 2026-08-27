-- Criptografa cpf/email de tb_usuario em repouso (AES-GCM via CryptoVaultService, mesmo padrão já
-- usado em tb_adv_clientes) e adiciona índice cego (HMAC-SHA256) para permitir a mesma busca por
-- igualdade que existia antes (login por CPF/e-mail, cruzamento de parte com processo, etc.), já
-- que o valor cifrado usa IV aleatório e nunca é comparável em WHERE.
--
-- As colunas cpf/email são alargadas para caber o texto cifrado (base64 de IV+ciphertext+tag).
-- As constraints UNIQUE antigas, sobre o texto agora cifrado, deixam de fazer sentido (todo valor
-- cifrado já é distinto por causa do IV aleatório — a constraint pararia de proteger duplicidade
-- real); a unicidade correta passa a viver no índice cego (cpf_hash/email_hash), que é determinístico.
--
-- Linhas existentes ficam com cpf_hash/email_hash NULL até o backfill (job) rodar — o índice UNIQUE
-- permite múltiplos NULL (comportamento padrão do Postgres), então nenhuma linha antiga quebra aqui.

ALTER TABLE tb_usuario ALTER COLUMN cpf TYPE VARCHAR(1000);
ALTER TABLE tb_usuario ALTER COLUMN email TYPE VARCHAR(1000);

ALTER TABLE tb_usuario ADD COLUMN cpf_hash VARCHAR(64);
ALTER TABLE tb_usuario ADD COLUMN email_hash VARCHAR(64);

ALTER TABLE tb_usuario DROP CONSTRAINT IF EXISTS uk_tb_usuario_cpf;
ALTER TABLE tb_usuario DROP CONSTRAINT IF EXISTS uk_tb_usuario_email;

CREATE UNIQUE INDEX uk_tb_usuario_cpf_hash ON tb_usuario (cpf_hash);
CREATE UNIQUE INDEX uk_tb_usuario_email_hash ON tb_usuario (email_hash);
