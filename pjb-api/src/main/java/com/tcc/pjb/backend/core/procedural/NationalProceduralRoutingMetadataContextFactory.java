package com.tcc.pjb.backend.core.procedural;

import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingMetadataContextFactory {

    NationalProceduralRoutingMetadataContext create(NationalProceduralRoutingCoreResolution resolution,
                                                    ProceduralEconomicGateReport economicGate) {
        Objects.requireNonNull(resolution);
        return new NationalProceduralRoutingMetadataContext(
                resolution.payload(),
                resolution.sourceLabel(),
                resolution.selectedRito().status(),
                resolution.selectedRito().metadata(),
                resolution.competence().debug(),
                resolution.teto(),
                distributionMetadata(resolution),
                resolution.partyProfile().toMap(),
                resolution.actionProfile().toMap(),
                resolution.juizadoDecision().toMap(),
                economicGate,
                resolution.judicialPlacement().forumAllocation(),
                resolution.actionProfile().actionNature(),
                resolution.actionProfile().actionFamily(),
                resolution.tipoJustica(),
                resolution.ritoSugerido(),
                resolution.complexityBand(),
                resolution.probatoryProfile(),
                resolution.reviewSynthesis().confidence(),
                resolution.reviewSynthesis().riskLevel(),
                Integer.toHexString(resolution.corpus().hashCode())
        );
    }

    private static Map<String, Object> distributionMetadata(NationalProceduralRoutingCoreResolution resolution) {
        if (resolution.judicialPlacement().distribution() == null) {
            return null;
        }
        return resolution.judicialPlacement().distribution().toMap();
    }
}
