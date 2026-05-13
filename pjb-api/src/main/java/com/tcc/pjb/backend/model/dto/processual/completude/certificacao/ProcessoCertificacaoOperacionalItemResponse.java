package com.tcc.pjb.backend.model.dto.processual.completude.certificacao;

public record ProcessoCertificacaoOperacionalItemResponse(
        String codigo,
        String categoria,
        String severidade,
        boolean conforme,
        String diagnostico,
        String acaoCorretiva
) {
}
