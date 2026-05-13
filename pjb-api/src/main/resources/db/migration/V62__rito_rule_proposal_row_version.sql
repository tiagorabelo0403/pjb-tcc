-- Controle de concorrência otimista para propostas (evita corridas na dupla aprovação)

ALTER TABLE tb_rito_rule_proposal
  ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;
