package com.tcc.pjb.backend.model.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

@Getter
public class IARunResult {

    private java.util.UUID runId;
    private String html;
    private double confidenceScore;
    private String riskCategory;
    private EssenceDecision essenceDecision;

    public IARunResult(java.util.UUID runId, String html, double confidenceScore, String riskCategory) {
        this.runId = runId;
        this.html = html;
        this.confidenceScore = confidenceScore;
        this.riskCategory = riskCategory;
    }

    public IARunResult withEssenceDecision(EssenceDecision decision) {
        this.essenceDecision = decision;
        return this;
    }

    @Getter
    public static class EssenceDecision {
        private boolean allowed;
        private List<String> violatedRules;
        private double differenceScore;

        
        private String justificativa;
        private LocalDateTime dataDecisao;
        private String decisor;
        private String categoria;
        private int severidade;
        private boolean recorrivel;

        
        public EssenceDecision(boolean allowed, List<String> violatedRules, double differenceScore) {
            this.allowed = allowed;
            this.violatedRules = violatedRules;
            this.differenceScore = differenceScore;
        }

        
        public EssenceDecision(boolean allowed, List<String> violatedRules, double differenceScore,
                               String justificativa, LocalDateTime dataDecisao, String decisor,
                               String categoria, int severidade, boolean recorrivel) {
            this.allowed = allowed;
            this.violatedRules = violatedRules;
            this.differenceScore = differenceScore;
            this.justificativa = justificativa;
            this.dataDecisao = dataDecisao;
            this.decisor = decisor;
            this.categoria = categoria;
            this.severidade = severidade;
            this.recorrivel = recorrivel;
        }

        public boolean allowed() { return allowed; }
    }
}