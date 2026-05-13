package com.tcc.pjb.backend.model.dto.processual.substituicao.nacional.cockpit;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoNacionalCockpitResponse(
        PjbSubstituicaoNacionalCockpitResumoResponse resumo,
        List<PjbSubstituicaoNacionalCockpitOndaResponse> ondas,
        List<PjbSubstituicaoNacionalCockpitTribunalResponse> tribunais,
        Instant geradoEm
) {
    public PjbSubstituicaoNacionalCockpitResponse {
        ondas = ondas == null ? List.of() : List.copyOf(ondas);
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
    }
}
