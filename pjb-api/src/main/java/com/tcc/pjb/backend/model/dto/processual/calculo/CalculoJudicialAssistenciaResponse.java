package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CalculoJudicialAssistenciaResponse(
        String dominio,
        CalculoJudicialSolicitantePerfil perfilResolvido,
        String titulo,
        String mensagemAbertura,
        List<String> mensagensAjuda,
        List<String> camposCriticosPendentes,
        List<String> validacoesBloqueantes,
        List<String> ajustesAutomaticosSugeridos,
        List<String> proximosPassos,
        Map<String, Object> autopreenchimentoSeguro,
        Map<String, Object> desenhoAssistido,
        List<String> guardrailsIa,
        Instant geradoEm
) {
}
