package com.tcc.pjb.backend.core.digitalizacao;

import java.util.List;

public record DocumentIntelligenceResult(
        String textoExtraido,
        double confiancaExtracao,
        String tipoDocumentoProvavel,
        List<String> inconsistenciasDetectadas,
        boolean documentoLegivel,
        boolean documentoDuplicadoSuspeito
) {
    public DocumentIntelligenceResult {
        inconsistenciasDetectadas = inconsistenciasDetectadas == null ? List.of() : List.copyOf(inconsistenciasDetectadas);
    }
}
