package com.tcc.pjb.backend.core.quality.codebase.domain;

import java.util.List;
import java.util.Objects;

public record PjbCodebaseExtractionBlueprint(
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
    public PjbCodebaseExtractionBlueprint {
        fatia = Objects.toString(fatia, "").trim();
        trilha = Objects.toString(trilha, "").trim();
        prontidao = Objects.toString(prontidao, "PREPARAR").trim();
        pacoteAlvo = Objects.toString(pacoteAlvo, "").trim();
        fachadaSugerida = Objects.toString(fachadaSugerida, "").trim();
        portaSugerida = Objects.toString(portaSugerida, "").trim();
        contratoIntegracaoSugerido = Objects.toString(contratoIntegracaoSugerido, "").trim();
        scorePrioridade = Math.max(0, scorePrioridade);
        bloqueios = bloqueios == null ? List.of() : List.copyOf(bloqueios);
        primeirasAcoes = primeirasAcoes == null ? List.of() : List.copyOf(primeirasAcoes);
    }
}
