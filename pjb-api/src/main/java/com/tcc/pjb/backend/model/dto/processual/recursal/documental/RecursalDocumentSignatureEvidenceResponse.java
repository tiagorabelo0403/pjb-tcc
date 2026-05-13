package com.tcc.pjb.backend.model.dto.processual.recursal.documental;

import java.util.List;

public record RecursalDocumentSignatureEvidenceResponse(
        String eixo,
        String titulo,
        String processoReferencia,
        String artefatoId,
        String modoAssinatura,
        String statusAssinatura,
        String temporalidade,
        String validacaoLongoPrazo,
        String envelopeProva,
        String politicaDocumental,
        List<String> cadeiaCertificados,
        List<String> provasAssociadas,
        List<String> alertasTaticos) {
}
