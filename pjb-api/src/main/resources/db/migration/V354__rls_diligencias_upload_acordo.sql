-- F7 (plano de melhoria v3): lote 7.
--
-- As 6 tabelas tb_diligencia_operador_* (V141-V145) sao trilha de execucao de diligencias sobre
-- um processo (certidao, juntada, formalizacao, anexacao institucional, encerramento, dispatch de
-- malha). Todas tem processo_id (nullable so em tb_diligencia_operador_encerramento, quando a
-- diligencia nao chegou a se vincular a um processo formal). Mesmo padrao EXISTS via
-- pjb_rls_processo_visivel (V348) ja usado nas satelites diretas -- funciona pra qualquer operador
-- (oficial de justica, servidor, etc.), nao so advogado: pjb_rls_processo_visivel ja e permissivo
-- quando o filtro de equipe nunca foi ativado (papel nao-advogado).
--
-- tb_upload_batch/tb_upload_item: lote de upload de arquivo vinculado a processo (batch tem
-- processo_id direto; item so tem batch_id, um salto).
--
-- tb_documento_pagina: pagina extraida de um documento_processual (V348), um salto via
-- documento_id.
--
-- tb_acordo_mensagem: mensagem de uma sessao de acordo processual (V349), um salto via sessao_id.
-- NAO repete a dimensao de visibilidade por mensagem (coluna visibilidade: PARTICIPANTES/
-- CONFIDENCIAL/MAGISTRADO/PUBLICA_PROCESSUAL) -- isso fica so na camada de aplicacao, mesmo
-- principio da V347 (RLS aqui e backstop de ownership de processo, nao substituto do controle
-- fino de visibilidade por mensagem).

ALTER TABLE tb_diligencia_operador_encerramento ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_diligencia_operador_encerramento FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_diligencia_operador_encerramento_processo_scope ON tb_diligencia_operador_encerramento
    USING (processo_id IS NULL OR pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_diligencia_operador_certidao_documento ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_diligencia_operador_certidao_documento FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_diligencia_operador_certidao_documento_processo_scope ON tb_diligencia_operador_certidao_documento
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_diligencia_operador_formalizacao_processual ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_diligencia_operador_formalizacao_processual FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_diligencia_operador_formalizacao_processual_processo_scope ON tb_diligencia_operador_formalizacao_processual
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_diligencia_operador_juntada_processual ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_diligencia_operador_juntada_processual FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_diligencia_operador_juntada_processual_processo_scope ON tb_diligencia_operador_juntada_processual
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_diligencia_operador_anexacao_institucional ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_diligencia_operador_anexacao_institucional FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_diligencia_operador_anexacao_institucional_processo_scope ON tb_diligencia_operador_anexacao_institucional
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_diligencia_operador_malha_institucional_dispatch ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_diligencia_operador_malha_institucional_dispatch FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_diligencia_operador_malha_institucional_dispatch_processo_scope ON tb_diligencia_operador_malha_institucional_dispatch
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_upload_batch ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_upload_batch FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_upload_batch_processo_scope ON tb_upload_batch
    USING (pjb_rls_processo_visivel(processo_id))
    WITH CHECK (true);

ALTER TABLE tb_upload_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_upload_item FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_upload_item_processo_scope ON tb_upload_item
    USING (
        EXISTS (
            SELECT 1 FROM tb_upload_batch b
            WHERE b.id = tb_upload_item.batch_id
              AND pjb_rls_processo_visivel(b.processo_id)
        )
    )
    WITH CHECK (true);

ALTER TABLE tb_documento_pagina ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_documento_pagina FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_documento_pagina_processo_scope ON tb_documento_pagina
    USING (
        EXISTS (
            SELECT 1 FROM tb_documento_processual d
            WHERE d.id = tb_documento_pagina.documento_id
              AND pjb_rls_processo_visivel(d.processo_id)
        )
    )
    WITH CHECK (true);

ALTER TABLE tb_acordo_mensagem ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_acordo_mensagem FORCE ROW LEVEL SECURITY;
CREATE POLICY tb_acordo_mensagem_processo_scope ON tb_acordo_mensagem
    USING (
        EXISTS (
            SELECT 1 FROM tb_sessao_acordo_processual s
            WHERE s.id = tb_acordo_mensagem.sessao_id
              AND pjb_rls_processo_visivel(s.processo_id)
        )
    )
    WITH CHECK (true);
