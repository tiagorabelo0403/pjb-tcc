package com.tcc.pjb.backend.core.procedural;

import java.util.List;
import java.util.Map;

public record NationalProceduralTribunalVariationRow(
        String tribunalCodigo,
        String unidadeCodigo,
        String rito,
        String ramo,
        String tipoJustica,
        String judicialSystem,
        boolean connectorOperational,
        boolean stepUpRequired,
        boolean certificateRequired,
        List<String> protocolChannels,
        List<String> unitAnchors,
        List<String> localRules,
        List<String> warnings,
        Map<String, Object> metadata
) {
    public NationalProceduralTribunalVariationRow {
        tribunalCodigo = normalize(tribunalCodigo, "PJB_PADRAO");
        unidadeCodigo = normalize(unidadeCodigo, "UNIDADE_A_DEFINIR");
        rito = normalize(rito, "COMUM_ORDINARIO");
        ramo = normalize(ramo, "CIVIL");
        tipoJustica = normalize(tipoJustica, "ESTADUAL");
        judicialSystem = normalize(judicialSystem, "OUTRO");
        protocolChannels = NationalProceduralRecordSupport.copyList(protocolChannels);
        unitAnchors = NationalProceduralRecordSupport.copyList(unitAnchors);
        localRules = NationalProceduralRecordSupport.copyList(localRules);
        warnings = NationalProceduralRecordSupport.copyList(warnings);
        metadata = NationalProceduralRecordSupport.copyMap(metadata);
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
