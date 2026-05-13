package com.tcc.pjb.backend.core.procedural;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ProceduralForumAllocationReport(
        Instant generatedAt,
        String classeTpuCodigo,
        String classeTpuNome,
        String competenciaTerritorialModo,
        String comarcaSugerida,
        String ufSugerida,
        String fundamentoTerritorial,
        String preventionMode,
        String linkageMode,
        List<String> relatedProcessNumbers,
        String tribunalCodigo,
        String tribunalNome,
        String unidadeJudiciariaCodigo,
        String varaSugerida,
        String tipoVaraSugerido,
        boolean varaEspecializada,
        boolean distribuicaoAutomatica,
        double distributionScore,
        String connectorSystem,
        boolean connectorOperational,
        boolean requiresGovBrStepUp,
        boolean requiresCertificate,
        boolean protocoloRealDisponivel,
        boolean preProtocoloApto,
        String preProtocoloStatus,
        List<String> incompatibilities,
        List<String> warnings,
        List<String> reviewChecklist,
        Map<String, Object> metadata
) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("generatedAt", generatedAt != null ? generatedAt.toString() : null);
        out.put("classeTpuCodigo", classeTpuCodigo);
        out.put("classeTpuNome", classeTpuNome);
        out.put("competenciaTerritorialModo", competenciaTerritorialModo);
        out.put("comarcaSugerida", comarcaSugerida);
        out.put("ufSugerida", ufSugerida);
        out.put("fundamentoTerritorial", fundamentoTerritorial);
        out.put("preventionMode", preventionMode);
        out.put("linkageMode", linkageMode);
        out.put("relatedProcessNumbers", relatedProcessNumbers == null ? List.of() : relatedProcessNumbers);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("tribunalNome", tribunalNome);
        out.put("unidadeJudiciariaCodigo", unidadeJudiciariaCodigo);
        out.put("varaSugerida", varaSugerida);
        out.put("tipoVaraSugerido", tipoVaraSugerido);
        out.put("varaEspecializada", varaEspecializada);
        out.put("distribuicaoAutomatica", distribuicaoAutomatica);
        out.put("distributionScore", distributionScore);
        out.put("connectorSystem", connectorSystem);
        out.put("connectorOperational", connectorOperational);
        out.put("requiresGovBrStepUp", requiresGovBrStepUp);
        out.put("requiresCertificate", requiresCertificate);
        out.put("protocoloRealDisponivel", protocoloRealDisponivel);
        out.put("preProtocoloApto", preProtocoloApto);
        out.put("preProtocoloStatus", preProtocoloStatus);
        out.put("incompatibilities", incompatibilities == null ? List.of() : incompatibilities);
        out.put("warnings", warnings == null ? List.of() : warnings);
        out.put("reviewChecklist", reviewChecklist == null ? List.of() : reviewChecklist);
        LinkedHashMap<String, Object> safeMetadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
        safeMetadata.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        out.put("metadata", safeMetadata);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
