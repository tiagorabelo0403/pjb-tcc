package com.tcc.pjb.backend.model.dto.processual.recursal.ia;

import com.tcc.pjb.backend.model.dto.processual.recursal.admissibilidade.RecursalAdmissibilityResponse;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record RecursalIaConferenciaResponse(
        String agente,
        String status,
        boolean conferenciaExecutada,
        String mensagemResultado,
        List<String> pendencias,
        List<String> bloqueios,
        List<String> alertasCriticos,
        List<String> ajustesAplicados,
        List<String> confirmacoesRecomendadas,
        Map<String, Object> metadata,
        RecursalAdmissibilityResponse admissibilidade,
        RecursalIaStructuredAnalysis analiseEstruturada,
        Instant geradoEm
) {
}
