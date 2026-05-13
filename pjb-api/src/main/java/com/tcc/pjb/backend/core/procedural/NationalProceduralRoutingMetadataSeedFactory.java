package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingMetadataSeedFactory {

    private final NationalProceduralJurisdictionIntakeResolver jurisdictionIntakeResolver;

    public NationalProceduralRoutingMetadataSeedFactory(NationalProceduralJurisdictionIntakeResolver jurisdictionIntakeResolver) {
        this.jurisdictionIntakeResolver = Objects.requireNonNull(jurisdictionIntakeResolver);
    }

    public Map<String, Object> build(NationalProceduralRoutingMetadataContext context) {
        Objects.requireNonNull(context);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("source", context.sourceLabel());
        metadata.put("canonicalStatus", context.canonicalStatus());
        metadata.put("canonicalMetadata", context.canonicalMetadata());
        metadata.put("competenceDebug", context.competenceDebug());
        metadata.put("teto", buildTetoMetadata(context.teto()));
        if (context.distributionMetadata() != null && !context.distributionMetadata().isEmpty()) {
            metadata.put("distribution", context.distributionMetadata());
        }
        metadata.put("partyProfile", context.partyProfileMetadata());
        metadata.put("actionProfile", context.actionProfileMetadata());
        metadata.put("juizadoDecision", context.juizadoDecisionMetadata());
        metadata.put("economicGate", context.economicGate() != null ? context.economicGate().toMap() : null);
        metadata.put("forumAllocation", context.forumAllocation() != null ? context.forumAllocation().toMap() : null);
        metadata.put("jurisdictionIntake", jurisdictionIntakeResolver.resolve(context).toMap());
        metadata.put("rightsCoverage", NationalProceduralRightsCatalogSupport.coverageFlags(context.ritoSugerido(), context.tipoJustica()));
        metadata.put("corpusFingerprint", context.corpusFingerprint());
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(metadata);
    }

    private Map<String, Object> buildTetoMetadata(com.tcc.pjb.backend.service.teto.TetoProcessualService.DiagnosticoTetoProcessual teto) {
        if (teto == null) {
            return null;
        }
        LinkedHashMap<String, Object> tetoMetadata = new LinkedHashMap<>();
        tetoMetadata.put("codigo", teto.codigoDiagnostico());
        tetoMetadata.put("violacao", teto.violacao());
        tetoMetadata.put("alerta", teto.alerta());
        tetoMetadata.put("bloqueante", teto.bloqueante());
        tetoMetadata.put("competenciaSugerida", teto.competenciaSugerida());
        tetoMetadata.put("ritoSugerido", teto.ritoSugerido());
        tetoMetadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Map.copyOf(tetoMetadata);
    }
}
