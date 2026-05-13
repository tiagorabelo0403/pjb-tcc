package com.tcc.pjb.backend.model.dto.processual.substituicao.migracao;

import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoMigracaoLoteSituacao;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
        Map<String, Object> snapshot,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public PjbSubstituicaoMigracaoLoteResponse {
        snapshot = snapshot == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(snapshot));
    }
}
