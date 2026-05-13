package com.tcc.pjb.backend.model.dto.processual.calculo;

import java.util.List;
import java.util.Map;

public record CalculoJudicialWorkspaceCardResponse(
        String codigo,
        String titulo,
        String descricao,
        String aba,
        List<String> perfisPermitidos,
        List<String> mensagensAjuda,
        List<String> secoes,
        List<String> automacoesSeguras,
        Map<String, String> rotas,
        Map<String, Object> design
) {
}
