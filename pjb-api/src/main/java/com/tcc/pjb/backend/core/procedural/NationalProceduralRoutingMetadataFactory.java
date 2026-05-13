package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingMetadataFactory {

    private final NationalProceduralRoutingMetadataSeedFactory metadataSeedFactory;
    private final NationalProceduralRoutingIntelligenceResolver intelligenceResolver;

    public NationalProceduralRoutingMetadataFactory(NationalProceduralRoutingMetadataSeedFactory metadataSeedFactory,
                                                    NationalProceduralRoutingIntelligenceResolver intelligenceResolver) {
        this.metadataSeedFactory = Objects.requireNonNull(metadataSeedFactory);
        this.intelligenceResolver = Objects.requireNonNull(intelligenceResolver);
    }

    public Map<String, Object> build(NationalProceduralRoutingMetadataContext context) {
        Objects.requireNonNull(context);
        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>(metadataSeedFactory.build(context));
        NationalProceduralRoutingIntelligenceBundle intelligence = intelligenceResolver.analyze(
                new NationalProceduralRoutingIntelligenceContext(
                        context.payload(),
                        context.canonicalMetadata(),
                        context.actionNature(),
                        context.actionFamily(),
                        context.tipoJustica(),
                        context.ritoSugerido(),
                        context.forumAllocation() != null ? context.forumAllocation().classeTpuCodigo() : null,
                        context.forumAllocation() != null ? context.forumAllocation().classeTpuNome() : null,
                        context.complexityBand(),
                        context.probatoryProfile(),
                        context.confidence(),
                        context.riskLevel()
                )
        );
        metadata.putAll(intelligence.toMetadataEntries());
        metadata.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(metadata);
    }
}
