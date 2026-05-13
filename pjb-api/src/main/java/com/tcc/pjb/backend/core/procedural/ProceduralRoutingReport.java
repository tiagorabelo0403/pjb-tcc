package com.tcc.pjb.backend.core.procedural;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralRoutingReport(
        Instant generatedAt,
        String actionNature,
        String actionFamily,
        String proceduralRegime,
        String proceduralTrack,
        String tipoJusticaSugerida,
        String ritoSugerido,
        String tribunalCodigo,
        String tribunalNome,
        String judicialSystem,
        String foroSugerido,
        String cidadeSugerida,
        String ufSugerida,
        String varaSugerida,
        String tipoVaraSugerido,
        String complexityBand,
        String probatoryProfile,
        boolean ritoEspecial,
        boolean admiteJuizado,
        boolean exigeRevisaoHumana,
        double confidence,
        String riskLevel,
        List<String> blockingIssues,
        ProceduralEconomicGateReport economicGate,
        ProceduralForumAllocationReport forumAllocation,
        List<String> reasons,
        List<String> legalBases,
        List<String> alerts,
        List<String> missingInputs,
        List<String> actionMarkers,
        List<String> reviewChecklist,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("actionNature", actionNature);
        out.put("actionFamily", actionFamily);
        out.put("proceduralRegime", proceduralRegime);
        out.put("proceduralTrack", proceduralTrack);
        out.put("tipoJusticaSugerida", tipoJusticaSugerida);
        out.put("ritoSugerido", ritoSugerido);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("tribunalNome", tribunalNome);
        out.put("judicialSystem", judicialSystem);
        out.put("foroSugerido", foroSugerido);
        out.put("cidadeSugerida", cidadeSugerida);
        out.put("ufSugerida", ufSugerida);
        out.put("varaSugerida", varaSugerida);
        out.put("tipoVaraSugerido", tipoVaraSugerido);
        out.put("complexityBand", complexityBand);
        out.put("probatoryProfile", probatoryProfile);
        out.put("ritoEspecial", ritoEspecial);
        out.put("admiteJuizado", admiteJuizado);
        out.put("exigeRevisaoHumana", exigeRevisaoHumana);
        out.put("confidence", confidence);
        out.put("riskLevel", riskLevel);
        out.put("blockingIssues", blockingIssues == null ? List.of() : blockingIssues);
        out.put("economicGate", economicGate == null ? Map.of() : economicGate.toMap());
        out.put("forumAllocation", forumAllocation == null ? Map.of() : forumAllocation.toMap());
        out.put("reasons", reasons == null ? List.of() : reasons);
        out.put("legalBases", legalBases == null ? List.of() : legalBases);
        out.put("alerts", alerts == null ? List.of() : alerts);
        out.put("missingInputs", missingInputs == null ? List.of() : missingInputs);
        out.put("actionMarkers", actionMarkers == null ? List.of() : actionMarkers);
        out.put("reviewChecklist", reviewChecklist == null ? List.of() : reviewChecklist);
        LinkedHashMap<String, Object> safeMetadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        safeMetadata.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        out.put("metadata", safeMetadata);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
