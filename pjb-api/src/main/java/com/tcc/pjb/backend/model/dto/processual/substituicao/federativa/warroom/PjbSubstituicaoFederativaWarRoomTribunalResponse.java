package com.tcc.pjb.backend.model.dto.processual.substituicao.federativa.warroom;

import java.util.List;

public record PjbSubstituicaoFederativaWarRoomTribunalResponse(
        String tribunalCodigo,
        String tribunalNome,
        String ramoJustica,
        String ondaAtual,
        String status,
        int scoreProntidao,
        boolean janelaAberta,
        boolean freezeAtivo,
        boolean corteLiberado,
        String janelaAtual,
        List<PjbSubstituicaoFederativaWarRoomRamoResponse> ramos,
        List<String> guardrails,
        List<String> rollback,
        List<String> bloqueadores,
        List<String> proximasAcoes
) {
    public PjbSubstituicaoFederativaWarRoomTribunalResponse {
        ramos = ramos == null ? List.of() : List.copyOf(ramos);
        guardrails = guardrails == null ? List.of() : List.copyOf(guardrails);
        rollback = rollback == null ? List.of() : List.copyOf(rollback);
        bloqueadores = bloqueadores == null ? List.of() : List.copyOf(bloqueadores);
        proximasAcoes = proximasAcoes == null ? List.of() : List.copyOf(proximasAcoes);
    }
}
