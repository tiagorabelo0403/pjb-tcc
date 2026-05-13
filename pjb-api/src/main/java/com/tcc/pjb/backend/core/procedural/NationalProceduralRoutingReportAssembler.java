package com.tcc.pjb.backend.core.procedural;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingReportAssembler {

    ProceduralRoutingReport assemble(NationalProceduralRoutingReportAssemblyContext context) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(context.actionProfile());
        Objects.requireNonNull(context.juizadoDecision());
        Objects.requireNonNull(context.reviewSynthesis());
        return new ProceduralRoutingReport(
                Instant.now(),
                context.actionProfile().actionNature(),
                context.actionProfile().actionFamily(),
                context.proceduralRegime(),
                context.proceduralTrack(),
                context.tipoJustica(),
                context.ritoSugerido(),
                context.tribunalCodigo(),
                context.tribunalNome(),
                context.judicialSystem(),
                context.foroSugerido(),
                context.cidadeSugerida(),
                context.ufSugerida(),
                context.varaSugerida(),
                context.tipoVaraSugerido(),
                context.complexityBand(),
                context.probatoryProfile(),
                context.actionProfile().specialProcedure() || !"COMUM".equals(context.proceduralRegime()),
                context.juizadoDecision().admiteJuizado(),
                context.reviewSynthesis().requiresHumanReview(),
                context.reviewSynthesis().confidence(),
                context.reviewSynthesis().riskLevel(),
                context.reviewSynthesis().blockingIssues(),
                context.economicGate(),
                context.forumAllocation(),
                context.reviewSynthesis().reasons(),
                context.reviewSynthesis().legalBases(),
                context.reviewSynthesis().alerts(),
                context.reviewSynthesis().missingInputs(),
                context.reviewSynthesis().actionMarkers(),
                context.reviewSynthesis().reviewChecklist(),
                freezeMetadata(context.metadata())
        );
    }

    private static Map<String, Object> freezeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Object> safe = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                safe.put(entry.getKey(), entry.getValue());
            }
        }
        return safe.isEmpty() ? Map.of() : Map.copyOf(safe);
    }
}
