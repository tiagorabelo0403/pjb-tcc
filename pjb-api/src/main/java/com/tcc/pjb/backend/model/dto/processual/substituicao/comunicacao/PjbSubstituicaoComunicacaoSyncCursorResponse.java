package com.tcc.pjb.backend.model.dto.processual.substituicao.comunicacao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoComunicacaoSyncSituacao;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

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
        @Schema(description = "Snapshot do cursor de sincronizacao — estado da migracao por sistema-fonte", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
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

