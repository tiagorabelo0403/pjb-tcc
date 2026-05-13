package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.malhajulgadora;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaMalhaJulgadoraResponse(
        int scoreNacional,
        boolean malhaJulgadoraPronta,
        boolean incidentesConectados,
        boolean colegiadosConectados,
        boolean unidadesJulgadorasConectadas,
        int tribunaisProntos,
        List<PjbSubstituicaoFederativaMalhaJulgadoraTribunalResponse> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaMalhaJulgadoraResponse {
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
