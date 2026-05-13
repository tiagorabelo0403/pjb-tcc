package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import java.util.Map;

record NationalProceduralRoutingIntelligenceContext(
        Map<String, Object> payload,
        Map<String, Object> canonicalMetadata,
        String actionNature,
        String actionFamily,
        TipoJustica tipoJustica,
        String ritoSugerido,
        String classeTpuCodigo,
        String classeTpuNome,
        String complexityBand,
        String probatoryProfile,
        double confidence,
        String riskLevel
) {}
