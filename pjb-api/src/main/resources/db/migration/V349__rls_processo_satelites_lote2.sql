-- F7 (plano de melhoria v3): lote 2 de tabelas-satelite de tb_processo, mesmo padrao da V348
-- (EXISTS via pjb_rls_processo_visivel, definida na V348).
--
-- tb_certidao_emitida.processo_id e' nullable (certidao pode ser emitida sem processo formal
-- vinculado, ex.: certidao de antecedentes avulsa); as demais tem processo_id NOT NULL.

ALTER TABLE pjb_ciencia_processual ENABLE ROW LEVEL SECURITY;
ALTER TABLE pjb_ciencia_processual FORCE ROW LEVEL SECURITY;
CREATE POLICY pjb_ciencia_processual_processo_scope ON pjb_ciencia_processual
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_certidao_emitida ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_certidao_emitida FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_certidao_emitida_processo_scope ON tb_certidao_emitida
    USING (processo_id IS NULL OR pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_expedicao_judicial ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_expedicao_judicial FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_expedicao_judicial_processo_scope ON tb_expedicao_judicial
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_processo_note ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_processo_note FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_processo_note_processo_scope ON tb_processo_note
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_sessao_acordo_processual ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_sessao_acordo_processual FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_sessao_acordo_processual_processo_scope ON tb_sessao_acordo_processual
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);
