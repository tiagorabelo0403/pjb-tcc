package com.tcc.pjb.backend.core.plataforma.sustentacao.digitaljustice;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.util.List;

public record PjbUniversalDigitalCoreDecision(
        RitoProcessual canonicalRito,
        PjbDigitalCoreRitoKind ritoKind,
        String routingAxis,
        String targetHierarchy,
        boolean routeToDigitalCore,
        boolean requiresHumanReview,
        boolean supportsRitoTransmutation,
        PjbRitoContext ritoContext,
        List<String> reasons,
        List<String> warnings
) {
}
