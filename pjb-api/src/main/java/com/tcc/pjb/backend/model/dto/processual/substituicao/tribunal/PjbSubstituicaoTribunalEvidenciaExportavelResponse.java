package com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record PjbSubstituicaoTribunalEvidenciaExportavelResponse(
        String tribunalCodigo,
        String nomeArquivo,
        String sha256,
        int tamanhoJson,
        int tamanhoGzip,
        Instant geradoEm,
        Map<String, Object> payload
) {
    public PjbSubstituicaoTribunalEvidenciaExportavelResponse {
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
}
