package com.tcc.pjb.backend.core.processo.unificado.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

public record ProcessoUnificadoCompetencia(
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
        String territorialMode,
        String preventionMode,
        String distributionMode,
        String specializationAxis,
        String allocationStrategy,
        String linkageMode,
        String competenceEnvelope,
        String routingRiskLevel,
        String suggestedDeskProfile,
        boolean sigiloPadrao,
        boolean conciliacaoObrigatoria,
        int prazoTriagemHoras,
        List<String> alertas,
        List<String> fundamentos,
        List<String> reviewChecklist,
        LinkedHashMap<String, Object> metadata
) {
    public ProcessoUnificadoCompetencia(String tipoJustica,
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
                                        String territorialMode,
                                        String preventionMode,
                                        String distributionMode,
                                        String specializationAxis,
                                        String allocationStrategy,
                                        String linkageMode,
                                        String competenceEnvelope,
                                        String suggestedDeskProfile,
                                        boolean sigiloPadrao,
                                        boolean conciliacaoObrigatoria,
                                        int prazoTriagemHoras,
                                        List<String> alertas,
                                        List<String> fundamentos,
                                        List<String> reviewChecklist,
                                        LinkedHashMap<String, Object> metadata) {
        this(tipoJustica, grauJurisdicao, ramoDireito, ritoProcessual, faseProcessual, statusProcessual, tribunalCodigo, tribunalNome,
                orgaoJulgadorSugerido, unidadeJudiciariaSugerida, filaDistribuicao, mesaTriagem, territorialMode, preventionMode,
                distributionMode, specializationAxis, allocationStrategy, linkageMode, competenceEnvelope, "NORMAL", suggestedDeskProfile,
                sigiloPadrao, conciliacaoObrigatoria, prazoTriagemHoras, alertas, fundamentos, reviewChecklist, metadata);
    }

    public ProcessoUnificadoCompetencia {
        Objects.requireNonNull(grauJurisdicao);
        Objects.requireNonNull(ramoDireito);
        Objects.requireNonNull(ritoProcessual);
        Objects.requireNonNull(faseProcessual);
        Objects.requireNonNull(statusProcessual);
        Objects.requireNonNull(alertas);
        Objects.requireNonNull(fundamentos);
        Objects.requireNonNull(reviewChecklist);
        alertas = List.copyOf(alertas);
        fundamentos = List.copyOf(fundamentos);
        reviewChecklist = List.copyOf(reviewChecklist);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }
}
