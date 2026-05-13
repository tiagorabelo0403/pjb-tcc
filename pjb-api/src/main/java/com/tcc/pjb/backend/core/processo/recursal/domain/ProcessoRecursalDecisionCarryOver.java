package com.tcc.pjb.backend.core.processo.recursal.domain;

import java.util.List;

public record ProcessoRecursalDecisionCarryOver(
        String scope,
        String sourceDecisionType,
        String sourceDecisionStage,
        String numeroProcessoOrigem,
        String numeroCnjOrigem,
        String tribunalOrigem,
        String unidadeOrigem,
        String classeProcessual,
        String assunto,
        String objetoProcessual,
        String pedidoPrincipal,
        String resultadoFinal,
        String resumoDecisorio,
        ProcessoRecursalDecisionSourceDocument documentoOriginalDecisao,
        String materialProbatorioResumo,
        String peticaoInicialResumo,
        String triagemResumo,
        String sourceTimelineMode,
        String targetTimelineMode,
        List<ProcessoRecursalDecisionLayer> trilhaDecisoriaIntegral,
        List<String> carryOverSignals,
        List<String> fundamentosExibicao
) {
    public ProcessoRecursalDecisionCarryOver {
        scope = scope == null || scope.isBlank() ? "DECISAO_ANTERIOR_VINCULADA" : scope;
        sourceDecisionType = sourceDecisionType == null || sourceDecisionType.isBlank() ? "DECISAO_JUDICIAL" : sourceDecisionType;
        sourceDecisionStage = sourceDecisionStage == null || sourceDecisionStage.isBlank() ? "MESMO_GRAU" : sourceDecisionStage;
        numeroProcessoOrigem = numeroProcessoOrigem == null ? null : numeroProcessoOrigem;
        numeroCnjOrigem = numeroCnjOrigem == null ? numeroProcessoOrigem : numeroCnjOrigem;
        trilhaDecisoriaIntegral = trilhaDecisoriaIntegral == null ? List.of() : List.copyOf(trilhaDecisoriaIntegral);
        carryOverSignals = carryOverSignals == null ? List.of() : List.copyOf(carryOverSignals);
        fundamentosExibicao = fundamentosExibicao == null ? List.of() : List.copyOf(fundamentosExibicao);
    }

    public boolean available() {
        return documentoOriginalDecisao != null && documentoOriginalDecisao.available()
                || trilhaDecisoriaIntegral.stream().anyMatch(ProcessoRecursalDecisionLayer::available)
                || !blank(resumoDecisorio)
                || !blank(resultadoFinal)
                || !blank(pedidoPrincipal)
                || !blank(objetoProcessual)
                || !blank(materialProbatorioResumo)
                || !blank(peticaoInicialResumo)
                || !blank(triagemResumo);
    }

    public String headline() {
        String core = documentoOriginalDecisao != null && documentoOriginalDecisao.available()
                ? documentoOriginalDecisao.displayTitle()
                : trilhaDecisoriaIntegral.stream().filter(ProcessoRecursalDecisionLayer::available).reduce((a, b) -> b).map(ProcessoRecursalDecisionLayer::headline).orElse(null);
        if (!blank(core)) {
            return core;
        }
        return !blank(resumoDecisorio) ? resumoDecisorio : !blank(resultadoFinal) ? resultadoFinal : !blank(pedidoPrincipal) ? pedidoPrincipal : objetoProcessual;
    }

    public String sourceOrganLabel() {
        if (!blank(unidadeOrigem) && !blank(tribunalOrigem)) {
            return unidadeOrigem + " / " + tribunalOrigem;
        }
        if (!blank(unidadeOrigem)) {
            return unidadeOrigem;
        }
        return tribunalOrigem;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
