package com.tcc.pjb.backend.model.dto.atoordinatorio;

import com.tcc.pjb.backend.model.entity.enums.TipoAtoOrdinatorio;
import java.util.Map;

public record AtoOrdinatorioResponse(
        Long documentoId,
        Long movimentacaoId,
        TipoAtoOrdinatorio tipo,
        String hash,
        Map<String, Object> assinaturaQualificada,
        Map<String, Object> validacaoSoberana
) {
}
