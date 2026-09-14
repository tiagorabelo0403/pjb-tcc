package com.tcc.pjb.backend.model.dto.processual.peticionamento.rascunho;

import java.time.Instant;

/**
 * Prévia somente-leitura de uma versão anterior do rascunho — o conteúdo dessa versão renderizado
 * com segurança, sem qualquer efeito colateral no rascunho atual (ao contrário de
 * {@code POST .../restaurar}, que sobrescreve o rascunho ativo). Fecha a lacuna de "ver antes de
 * restaurar": até aqui, {@code GET /versoes} só devolvia metadados (título, hash, tamanho), e a única
 * forma de ver o conteúdo de uma versão era restaurá-la de vez.
 */
public record DraftVersaoPreviewResponse(
        Long draftId,
        int versaoSeq,
        String origem,
        String tituloCaso,
        String conteudoHtml,
        String origemConteudo,
        String hashIntegridade,
        Instant createdAt
) {
}
