package com.tcc.pjb.backend.service.processual.peticionamento.leitura;

import java.time.Instant;

/**
 * Superfície de leitura da peça inicial publicada: o MESMO conteúdo validado que o peticionante
 * escreveu no editor, renderizado com segurança para quem lê a peça no processo (juiz, servidor,
 * parte, público autorizado). O corpo HTML é sempre derivado de fonte sanitizada — nunca do
 * cliente — e a exposição é gateada pelo mesmo ABAC/sigilo do download de documento.
 *
 * <p>{@code origemConteudo}: {@code JSON_SANITIZADO} quando renderizado do {@code conteudo_json}
 * autoritativo; {@code MINUTA_TEXTO} quando não há JSON e a minuta legada é escapada como texto;
 * {@code VAZIO} quando não há conteúdo.</p>
 */
public record PecaInicialLeituraResponse(
        Long processoId,
        String numeroProcesso,
        String tituloCaso,
        String rito,
        String conteudoHtml,
        String origemConteudo,
        boolean sigiloso,
        Instant atualizadoEm
) {
}
