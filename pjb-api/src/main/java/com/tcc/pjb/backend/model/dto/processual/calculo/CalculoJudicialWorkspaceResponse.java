package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CalculoJudicialWorkspaceResponse(
        String abaPadrao,
        String titulo,
        String subtitulo,
        CalculoJudicialSolicitantePerfil perfilResolvido,
        List<String> abasDisponiveis,
        List<String> mensagensGlobais,
        List<CalculoJudicialWorkspaceCardResponse> calculadoras,
        List<String> comportamentoDiario,
        List<String> guardrailsIa,
        Map<String, Object> designNavegacao,
        Instant geradoEm
) {
}
