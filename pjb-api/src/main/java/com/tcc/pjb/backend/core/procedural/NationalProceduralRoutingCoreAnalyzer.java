package com.tcc.pjb.backend.core.procedural;

import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralRoutingCoreAnalyzer {

    private final NationalProceduralRoutingFoundationAnalyzer foundationAnalyzer;
    private final NationalProceduralRoutingClassificationAnalyzer classificationAnalyzer;

    public NationalProceduralRoutingCoreAnalyzer(NationalProceduralRoutingFoundationAnalyzer foundationAnalyzer,
                                                 NationalProceduralRoutingClassificationAnalyzer classificationAnalyzer) {
        this.foundationAnalyzer = Objects.requireNonNull(foundationAnalyzer);
        this.classificationAnalyzer = Objects.requireNonNull(classificationAnalyzer);
    }

    NationalProceduralRoutingCoreResolution analyze(Map<String, Object> payload, String sourceLabel) {
        NationalProceduralRoutingFoundationResolution foundation = foundationAnalyzer.analyze(payload, sourceLabel);
        return classificationAnalyzer.analyze(foundation);
    }
}
