package com.tcc.pjb.backend.model.dto.processual.surface.unificado;

import java.util.List;
import java.util.Map;

public record ProcessoSurfaceCompetenciaResponse(
        String tipoJustica,
        String grauJurisdicao,
        String ramoDireito,
        String ritoProcessual,
        String faseProcessual,
        String statusProcessual,
        String tribunalCodigo,
        String tribunalNome,
        String orgaoJulgadorSugerido,
        String unidadeJudiciariaSugerida,
        String filaDistribuicao,
        String mesaTriagem,
        String preventionMode,
        String distributionMode,
        String routingRiskLevel,
        boolean sigiloPadrao,
        boolean conciliacaoObrigatoria,
        int prazoTriagemHoras,
        List<String> alertas,
        List<String> fundamentos,
        List<String> reviewChecklist,
        Map<String, Object> metadata
) {
}
