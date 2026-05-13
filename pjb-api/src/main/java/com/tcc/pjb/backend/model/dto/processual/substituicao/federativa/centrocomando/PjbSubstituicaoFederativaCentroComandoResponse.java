package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.centrocomando;

import com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.common.PjbSubstituicaoFederativaTribunalResponse;
import java.time.Instant;
import java.util.List;

public record PjbSubstituicaoFederativaCentroComandoResponse(
        int scoreNacional,
        boolean prontoRolloutFederativo,
        boolean prontoRollbackGovernado,
        int tribunaisMonitorados,
        int tribunaisProntosCorteAssistido,
        int tribunaisComBloqueio,
        List<String> pendenciasCriticas,
        List<PjbSubstituicaoFederativaTribunalResponse> tribunais,
        List<String> fundamentos,
        Instant geradoEm
) {
    public PjbSubstituicaoFederativaCentroComandoResponse {
        pendenciasCriticas = pendenciasCriticas == null ? List.of() : List.copyOf(pendenciasCriticas);
        tribunais = tribunais == null ? List.of() : List.copyOf(tribunais);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
