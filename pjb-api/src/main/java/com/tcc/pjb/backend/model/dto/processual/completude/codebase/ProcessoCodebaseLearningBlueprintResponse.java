package com.tcc.pjb.backend.model.dto.processual.completude.codebase;

import java.util.List;

public record ProcessoCodebaseLearningBlueprintResponse(
        String fatia,
        String trilha,
        String prontidao,
        int scorePrioridade,
        String pacoteAlvo,
        String fachadaSugerida,
        String portaSugerida,
        String contratoIntegracaoSugerido,
        List<String> bloqueios,
        List<String> primeirasAcoes
) {
    public ProcessoCodebaseLearningBlueprintResponse {
        bloqueios = bloqueios == null ? List.of() : List.copyOf(bloqueios);
        primeirasAcoes = primeirasAcoes == null ? List.of() : List.copyOf(primeirasAcoes);
    }
}
