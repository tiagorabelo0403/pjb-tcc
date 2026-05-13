package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.util.Map;

record NationalProceduralRoutingMetadataContext(
        Map<String, Object> payload,
        String sourceLabel,
        String canonicalStatus,
        Map<String, Object> canonicalMetadata,
        Map<String, Object> competenceDebug,
        TetoProcessualService.DiagnosticoTetoProcessual teto,
        Map<String, Object> distributionMetadata,
        Map<String, Object> partyProfileMetadata,
        Map<String, Object> actionProfileMetadata,
        Map<String, Object> juizadoDecisionMetadata,
        ProceduralEconomicGateReport economicGate,
        ProceduralForumAllocationReport forumAllocation,
        String actionNature,
        String actionFamily,
        TipoJustica tipoJustica,
        String ritoSugerido,
        String complexityBand,
        String probatoryProfile,
        double confidence,
        String riskLevel,
        String corpusFingerprint
) {}
