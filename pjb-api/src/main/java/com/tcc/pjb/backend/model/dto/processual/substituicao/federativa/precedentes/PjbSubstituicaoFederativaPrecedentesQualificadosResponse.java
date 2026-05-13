package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.precedentes;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaPrecedentesQualificadosResponse(
        int scoreNacional,
        boolean malhaPrecedentesPronta,
        boolean incidentesMassaConectados,
        boolean temasAfetadosGovernados,
        boolean sobrestamentoGovernado,
        boolean precedentesVinculantesConectados,
        int tribunaisProntos,
        List<PjbSubstituicaoFederativaPrecedentesQualificadosTribunalResponse> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaPrecedentesQualificadosResponse {
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
