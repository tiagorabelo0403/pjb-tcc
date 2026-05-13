package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.dto.competencia.CompetenceResolveResponse;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.util.Collection;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

@Component
public class NationalProceduralConfidenceResolver {

    NationalProceduralConfidenceAssessment assess(CanonicalRitoSelector.SelectedRito selectedRito,
                                                  CompetenceResolveResponse competence,
                                                  NationalProceduralJuizadoDecision juizadoDecision,
                                                  ProceduralForumAllocationReport forumAllocation,
                                                  NationalProceduralDistributionSuggestion distribution,
                                                  Collection<String> missingInputs,
                                                  Collection<String> alerts,
                                                  TetoProcessualService.DiagnosticoTetoProcessual teto) {
        double confidence = weightedConfidence(selectedRito, competence, juizadoDecision, distribution, missingInputs);
        if (forumAllocation != null) {
            if (forumAllocation.preProtocoloApto()) {
                confidence += 0.04d;
            }
            if (forumAllocation.distribuicaoAutomatica()) {
                confidence += 0.03d;
            } else {
                confidence -= 0.05d;
            }
            if (!forumAllocation.connectorOperational()) {
                confidence -= 0.07d;
            }
            if (!forumAllocation.incompatibilities().isEmpty()) {
                confidence -= Math.min(0.22d, forumAllocation.incompatibilities().size() * 0.05d);
            }
            if (!forumAllocation.relatedProcessNumbers().isEmpty() && !"NENHUM_SINAL".equals(forumAllocation.linkageMode())) {
                confidence -= 0.03d;
            }
        }
        confidence = round(Math.max(0.12d, Math.min(0.99d, confidence)));
        boolean requiresHumanReview = confidence < 0.73d
                || missingInputs != null && !missingInputs.isEmpty()
                || teto.bloqueante()
                || selectedRito.sanityGate() != null && selectedRito.sanityGate().hasBlockingIssues()
                || juizadoDecision.requiresReview()
                || forumAllocation == null
                || !forumAllocation.preProtocoloApto()
                || !forumAllocation.distribuicaoAutomatica();
        String riskLevel = forumAllocation != null && !forumAllocation.incompatibilities().isEmpty()
                ? "CRITICO"
                : resolveRiskLevel(confidence, requiresHumanReview, teto, alerts);
        return new NationalProceduralConfidenceAssessment(confidence, requiresHumanReview, riskLevel);
    }

    private double weightedConfidence(CanonicalRitoSelector.SelectedRito selectedRito,
                                      CompetenceResolveResponse competence,
                                      NationalProceduralJuizadoDecision juizadoDecision,
                                      NationalProceduralDistributionSuggestion distribution,
                                      Collection<String> missingInputs) {
        double canonicalWeight = selectedRito.fallbackApplied() ? 0.42d : selectedRito.heuristicUsed() ? 0.58d : 0.74d;
        double competenceWeight = competence.confidence();
        double juizadoWeight = juizadoDecision.confidence();
        double distributionWeight = distribution == null ? 0.58d : Math.max(0.55d, distribution.scoreFinal());
        int missingCount = missingInputs == null ? 0 : missingInputs.size();
        double score = canonicalWeight * 0.32d + competenceWeight * 0.28d + juizadoWeight * 0.20d + distributionWeight * 0.20d;
        score -= Math.min(0.24d, missingCount * 0.035d);
        return Math.max(0.18d, Math.min(0.99d, score));
    }

    private String resolveRiskLevel(double confidence,
                                    boolean requiresHumanReview,
                                    TetoProcessualService.DiagnosticoTetoProcessual teto,
                                    Collection<String> alerts) {
        if (teto.bloqueante()) {
            return "CRITICO";
        }
        if (requiresHumanReview && confidence < 0.60d) {
            return "ALTO";
        }
        if (alerts != null && !alerts.isEmpty() || confidence < 0.76d || teto.alerta()) {
            return "MODERADO";
        }
        return "BAIXO";
    }

    private static double round(double value) {
        return NationalProceduralRoutingSupport.round(value);
    }
}
