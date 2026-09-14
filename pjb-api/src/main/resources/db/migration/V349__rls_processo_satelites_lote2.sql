-- F7 (plano de melhoria v3): lote 2 de tabelas-satelite de tb_processo, mesmo padrao da V348
-- (EXISTS via pjb_rls_processo_visivel, definida na V348).
--
-- tb_certidao_emitida nao entra: a V345 (F2, tabelas orfas) ja tinha derrubado essa tabela como
-- duplicata de tb_diligencia_operador_certidao_documento (V141, ainda viva) antes desta V349 ser
-- escrita -- as duas fatias avancaram em paralelo sobre o mesmo master antigo sem se cruzar. Zero
-- referencia Java a tb_certidao_emitida confirma que o DROP da V345 estava certo; quem estava
-- desatualizada era esta migration.

ALTER TABLE pjb_ciencia_processual ENABLE ROW LEVEL SECURITY;
ALTER TABLE pjb_ciencia_processual FORCE ROW LEVEL SECURITY;
CREATE POLICY pjb_ciencia_processual_processo_scope ON pjb_ciencia_processual
    USING (pjb_rls_processo_visivel(processo_id))
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
