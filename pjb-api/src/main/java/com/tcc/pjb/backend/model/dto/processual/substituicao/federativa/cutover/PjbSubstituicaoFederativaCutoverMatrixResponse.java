package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.cutover;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaCutoverMatrixResponse(
        int scoreGeral,
        boolean freezeNacionalAtivo,
        boolean prontoJanelaMaterial,
        int tribunaisLiberados,
        int competenciasLiberadas,
        List<PjbSubstituicaoFederativaCutoverTribunalResponse> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaCutoverMatrixResponse {
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
