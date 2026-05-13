package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.nucleoduro;

import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaNucleoDuroResponse(
        int scoreNacional,
        boolean prontoNucleoDuro,
        boolean comunicacaoSigiloConectados,
        boolean prevencaoRedistribuicaoConectadas,
        boolean fluxoRecursalConectado,
        int tribunaisProntosNucleoDuro,
        List<PjbSubstituicaoFederativaNucleoDuroTribunalResponse> tribunais,
        List<String> bloqueadoresCriticos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaNucleoDuroResponse {
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        bloqueadoresCriticos = bloqueadoresCriticos == null ? List.of() : List.copyOf(bloqueadoresCriticos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
