package com.tcc.pjb.backend.model.dto.processual.substituicao.comunicacao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoComunicacaoSyncSituacao;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record PjbSubstituicaoComunicacaoSyncCursorResponse(
        Long cursorId,
        String tribunalCodigo,
        String canalOrigem,
        Instant janelaInicio,
        Instant janelaFim,
        String correlationNamespace,
        String dedupeNamespace,
        PjbSubstituicaoComunicacaoSyncSituacao situacao,
        int totalRecebido,
        int totalDeduplicado,
        int totalCorrelacionado,
        int totalReprocessavel,
        Map<String, Object> snapshot,
        List<PjbSubstituicaoComunicacaoSyncItemResponse> itens,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public PjbSubstituicaoComunicacaoSyncCursorResponse {
        snapshot = snapshot == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(snapshot));
        itens = itens == null ? List.of() : List.copyOf(itens);
    }
}
