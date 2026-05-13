package com.tcc.pjb.backend.core.kernel.advisory;

final class ProcessMaterialStrategyScoringPolicy {

    private final ProcessMaterialStrategyTextSupport textSupport;

    ProcessMaterialStrategyScoringPolicy(ProcessMaterialStrategyTextSupport textSupport) {
        this.textSupport = textSupport;
    }

    int inferScore(String bracket) {
        String normalized = textSupport.normalize(bracket);
        if (textSupport.containsAny(normalized, "FORTE", "ALTA")) {
            return 82;
        }
        if (textSupport.containsAny(normalized, "MODERADA")) {
            return 61;
        }
        if (textSupport.containsAny(normalized, "RESTRITA", "INICIAL")) {
            return 38;
        }
        return 50;
    }

    int normalizeReadiness(Double readinessScore,
                           int gapCount,
                           int anchorCount,
                           int checklistCount,
                           boolean urgent,
                           String authorId,
                           String counterpartyId) {
        if (readinessScore != null) {
            double normalized = readinessScore > 1.0d ? readinessScore : readinessScore * 100.0d;
            return clamp((int) Math.round(normalized), 0, 100);
        }
        int score = 54;
        score += Math.min(18, anchorCount * 4);
        score += Math.min(10, checklistCount * 2);
        score -= Math.min(24, gapCount * 6);
        if (this.textSupport.blank(authorId)) {
            score -= 8;
        }
        if (this.textSupport.blank(counterpartyId)) {
            score -= 6;
        }
        if (urgent && anchorCount < 2) {
            score -= 8;
        }
        return clamp(score, 0, 100);
    }

    int normalizeScore(Integer explicitScore,
                       String bracket,
                       int positiveSignals,
                       int gapCount) {
        if (explicitScore != null) {
            return clamp(explicitScore, 0, 100);
        }
        int score = inferScore(bracket);
        score += Math.min(12, positiveSignals * 2);
        score -= Math.min(18, gapCount * 3);
        return clamp(score, 0, 100);
    }

    String classifyLitigationPosture(int evidenceScore, int negotiationScore, int gapCount, boolean urgent) {
        if (gapCount >= 4 || evidenceScore < 45) {
            return urgent ? "SANEAMENTO_PROBATORIO_URGENTE" : "SANEAMENTO_PROBATORIO";
        }
        if (urgent && evidenceScore >= 60) {
            return "ASSERTIVA_CONTROLADA";
        }
        if (negotiationScore >= 68 && evidenceScore >= 58) {
            return "BIFRONTE_CONTENCIOSA_NEGOCIAL";
        }
        if (evidenceScore >= 75) {
            return "IMPULSO_CONTENCIOSO_ESTRUTURADO";
        }
        return "CONTENCAO_ESTRATEGICA";
    }

    String classifyProtocolReadiness(int readinessScore, int blockerCount) {
        if (readinessScore >= 80) {
            return "APTO_SUPERVISIONADO";
        }
        if (blockerCount >= 2 || readinessScore < 55) {
            return "BLOQUEADO";
        }
        if (blockerCount == 1 || readinessScore < 78) {
            return "REVISAR";
        }
        return "APTO_SUPERVISIONADO";
    }

    String classifyNegotiationStance(int negotiationScore, int gapCount, boolean urgent) {
        if (negotiationScore >= 75 && gapCount <= 2) {
            return urgent ? "EXECUCAO_IMEDIATA_CONTROLADA" : "EXPANSIVA_CONTROLADA";
        }
        if (negotiationScore >= 58) {
            return "BALANCEADA";
        }
        return "CAUTELOSA";
    }

    String classifyEvidenceReadiness(int evidenceScore, int gapCount) {
        if (evidenceScore >= 78 && gapCount <= 2) {
            return "ROBUSTA";
        }
        if (evidenceScore >= 58) {
            return "SUFICIENTE_EM_EVOLUCAO";
        }
        return "FRAGIL";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
