-- evidence_chave_custodia recebe Hashes.sha256Hex(...) — 64 caracteres hex — produzido em
-- DocumentTrustChainService:130. A V355 alargou apenas tb_cadeia_custodia_digital; estas tres
-- tabelas ficaram com o mesmo nome de coluna, o mesmo conceito e varchar(32).
--
-- Nao confundir com a coluna chave_custodia (sem prefixo), que guarda "proc:<id>" e continua
-- corretamente em varchar(32). Alargar aquela por semelhanca de nome seria cargo cult.
ALTER TABLE tb_diligencia_operador_certidao
    ALTER COLUMN evidence_chave_custodia TYPE varchar(64);

ALTER TABLE tb_diligencia_operador_formalizacao_processual
    ALTER COLUMN evidence_chave_custodia TYPE varchar(64);

ALTER TABLE tb_diligencia_operador_juntada_processual
    ALTER COLUMN evidence_chave_custodia TYPE varchar(64);
