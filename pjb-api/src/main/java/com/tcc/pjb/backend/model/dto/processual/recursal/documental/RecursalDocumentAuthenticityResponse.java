package com.tcc.pjb.backend.model.dto.processual.recursal.documental;

import java.util.List;

public record RecursalDocumentAuthenticityResponse(
        String eixo,
        String titulo,
        String processoReferencia,
        String artefatoId,
        String envelopeProva,
        String politicaAcesso,
        String algoritmoHash,
        String hashReferencia,
        String modoValidacao,
        String statusAutenticidade,
        String rotaConferenciaPublica,
        String rotaLinhaAssinatura,
        List<String> provasAssociadas,
        List<String> alertasTaticos) {
}
