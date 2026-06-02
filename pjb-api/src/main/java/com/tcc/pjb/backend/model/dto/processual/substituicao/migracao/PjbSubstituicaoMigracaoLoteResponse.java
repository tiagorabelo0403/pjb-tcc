package com.tcc.pjb.backend.model.dto.processual.substituicao.migracao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoMigracaoLoteSituacao;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record PjbSubstituicaoMigracaoLoteResponse(
        Long loteId,
        String tribunalCodigo,
        String loteCodigo,
        int loteOrdem,
        String faixaReferencia,
        int totalItens,
        PjbSubstituicaoMigracaoLoteSituacao situacao,
        String checksumEsperado,
        String checksumApurado,
        int divergencias,
        @Schema(description = "Snapshot do lote de migracao — estado acumulado por fase de migracao", implementation = Object.class)
        @Size(max = 30)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Object> snapshot,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public PjbSubstituicaoMigracaoLoteResponse {
        snapshot = snapshot == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(snapshot));
    }
}

