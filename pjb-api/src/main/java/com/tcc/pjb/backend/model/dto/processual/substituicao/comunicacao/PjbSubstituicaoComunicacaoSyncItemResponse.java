package com.tcc.pjb.backend.model.dto.processual.substituicao.comunicacao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoComunicacaoSyncSituacao;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record PjbSubstituicaoComunicacaoSyncItemResponse(
        Long itemId,
        String dedupeHash,
        String externalMessageId,
        String correlationKey,
        String processoNumero,
        PjbSubstituicaoComunicacaoSyncSituacao situacao,
        boolean reprocessavel,
        @Schema(description = "Payload de sincronizacao — estrutura definida pelo sistema PJe legado", implementation = Object.class)
        @Size(max = 50)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> payload,
        @Schema(description = "Resultado da sincronizacao — estrutura definida pelo sistema PJe legado", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> resultado,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public PjbSubstituicaoComunicacaoSyncItemResponse {
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
        resultado = resultado == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(resultado));
    }
}

