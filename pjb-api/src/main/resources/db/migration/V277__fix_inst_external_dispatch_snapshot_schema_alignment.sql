ALTER TABLE tb_inst_external_dispatch_snapshot
    ADD COLUMN IF NOT EXISTS unidade_codigo VARCHAR(180);

UPDATE tb_inst_external_dispatch_snapshot
   SET unidade_codigo = 'UNKNOWN'
 WHERE unidade_codigo IS NULL;

ALTER TABLE tb_inst_external_dispatch_snapshot
    ALTER COLUMN unidade_codigo SET NOT NULL;

ALTER TABLE tb_inst_external_dispatch_snapshot
    ADD COLUMN IF NOT EXISTS destinatario_kind_codigo VARCHAR(80);

UPDATE tb_inst_external_dispatch_snapshot
   SET destinatario_kind_codigo = 'UNKNOWN'
 WHERE destinatario_kind_codigo IS NULL;

ALTER TABLE tb_inst_external_dispatch_snapshot
    ALTER COLUMN destinatario_kind_codigo SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_inst_external_dispatch_unidade_kind
    ON tb_inst_external_dispatch_snapshot (unidade_codigo, destinatario_kind_codigo, status_codigo);
