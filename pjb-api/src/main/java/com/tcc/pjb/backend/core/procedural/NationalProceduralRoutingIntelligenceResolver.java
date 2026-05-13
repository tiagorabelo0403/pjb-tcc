package com.tcc.pjb.backend.core.procedural;

import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingIntelligenceResolver {

    public NationalProceduralRoutingIntelligenceBundle analyze(NationalProceduralRoutingIntelligenceContext context) {
        Objects.requireNonNull(context);
        ProceduralIntelligenceAdvisoryReport advisoryIntelligence = ProceduralIntelligenceAdvisor.analyzeRouting(
                context.payload(),
                context.actionNature(),
                context.actionFamily(),
                context.tipoJustica(),
                context.ritoSugerido(),
                context.classeTpuCodigo(),
                context.classeTpuNome(),
                context.complexityBand(),
                context.probatoryProfile(),
                context.confidence(),
                context.riskLevel()
        );
        ProceduralDecisionQualityReport decisionQuality = ProceduralDecisionQualityEngine.analyze(
                context.payload(),
                context.canonicalMetadata(),
                context.actionNature(),
                context.actionFamily(),
                context.tipoJustica(),
                context.ritoSugerido(),
                context.riskLevel(),
                context.confidence(),
                advisoryIntelligence
        );
        ProceduralAutomationPolicyReport automationPolicy = ProceduralAutomationPolicyEngine.analyze(
                context.payload(),
                context.actionNature(),
                context.actionFamily(),
                context.tipoJustica(),
                context.ritoSugerido(),
                context.riskLevel(),
                advisoryIntelligence,
                decisionQuality
        );
        ProceduralExecutiveExplainabilityReport executiveExplainability = ProceduralExecutiveExplainabilityService.analyze(
                context.payload(),
                context.actionNature(),
                context.actionFamily(),
                context.riskLevel(),
                advisoryIntelligence,
                decisionQuality,
                automationPolicy
        );
        ProceduralAccelerationReport acceleration = ProceduralAccelerationEngine.analyze(
                context.payload(),
                context.actionNature(),
                context.actionFamily(),
                context.tipoJustica(),
                context.ritoSugerido(),
                context.riskLevel(),
                advisoryIntelligence,
                decisionQuality,
                automationPolicy,
                executiveExplainability
        );
        return new NationalProceduralRoutingIntelligenceBundle(
                advisoryIntelligence,
                decisionQuality,
                automationPolicy,
                executiveExplainability,
                acceleration
        );
    }
}
