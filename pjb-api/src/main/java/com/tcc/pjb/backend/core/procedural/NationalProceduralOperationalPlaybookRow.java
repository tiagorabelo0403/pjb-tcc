package com.tcc.pjb.backend.core.procedural;

import java.util.List;
import java.util.Map;

public record NationalProceduralOperationalPlaybookRow(
        String rito,
        String ramo,
        String grupo,
        String protocoloSugerido,
        List<String> competenceTracks,
        List<String> preProtocolChecklist,
        List<String> unitAnchors,
        List<String> requiredDocuments,
        List<String> guarantees,
        List<String> warnings,
        List<NationalProceduralOperationalPlaybookStep> steps,
        Map<String, Object> metadata
) {
    public NationalProceduralOperationalPlaybookRow {
        rito = normalize(rito, "COMUM_ORDINARIO");
        ramo = normalize(ramo, "CIVIL");
        grupo = normalize(grupo, "CIVIL");
        protocoloSugerido = normalize(protocoloSugerido, "PJB");
        competenceTracks = NationalProceduralRecordSupport.copyList(competenceTracks);
        preProtocolChecklist = NationalProceduralRecordSupport.copyList(preProtocolChecklist);
        unitAnchors = NationalProceduralRecordSupport.copyList(unitAnchors);
        requiredDocuments = NationalProceduralRecordSupport.copyList(requiredDocuments);
        guarantees = NationalProceduralRecordSupport.copyList(guarantees);
        warnings = NationalProceduralRecordSupport.copyList(warnings);
        steps = NationalProceduralRecordSupport.copyList(steps);
        metadata = NationalProceduralRecordSupport.copyMap(metadata);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
