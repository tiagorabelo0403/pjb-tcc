package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record CalculoJudicialIaFinanceiraResponse(
        String agente,
        String dominio,
        CalculoJudicialSolicitantePerfil perfilResolvido,
        String status,
        boolean calculoExecutado,
        String mensagemAbertura,
        String mensagemResultado,
        List<String> guardrails,
        List<String> pendencias,
        List<String> bloqueios,
        List<String> ajustesAplicados,
        List<String> confirmacoesRecomendadas,
        Map<String, Object> autopreenchimentoAplicado,
        Map<String, Object> metadata,
        CalculoJudicialAssistenciaResponse assistencia,
        CalculoJudicialResumoResponse resultado,
        Instant geradoEm
) {
}
