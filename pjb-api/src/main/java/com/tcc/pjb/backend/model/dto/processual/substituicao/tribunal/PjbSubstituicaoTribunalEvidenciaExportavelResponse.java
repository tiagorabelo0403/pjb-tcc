package com.tcc.pjb.backend.model.dto.processual.substituicao.tribunal;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record PjbSubstituicaoTribunalEvidenciaExportavelResponse(
        String tribunalCodigo,
        String nomeArquivo,
        String sha256,
        int tamanhoJson,
        int tamanhoGzip,
        Instant geradoEm,
        @Schema(description = "Payload de evidencia exportavel — estrutura definida pelo sistema-alvo de destino", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> payload
) {
    public PjbSubstituicaoTribunalEvidenciaExportavelResponse {
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }
}

