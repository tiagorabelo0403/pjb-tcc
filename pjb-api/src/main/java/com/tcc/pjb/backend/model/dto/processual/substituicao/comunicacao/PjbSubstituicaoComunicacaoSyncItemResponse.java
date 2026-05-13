package com.tcc.pjb.backend.model.dto.processual.substituicao.comunicacao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoComunicacaoSyncSituacao;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record PjbSubstituicaoComunicacaoSyncItemResponse(
        Long itemId,
        String dedupeHash,
        String externalMessageId,
        String correlationKey,
        String processoNumero,
        PjbSubstituicaoComunicacaoSyncSituacao situacao,
        boolean reprocessavel,
        Map<String, Object> payload,
        Map<String, Object> resultado,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public PjbSubstituicaoComunicacaoSyncItemResponse {
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
        resultado = resultado == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(resultado));
    }
}
