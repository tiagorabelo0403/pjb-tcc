package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record NationalProceduralPartyProfile(
        boolean federal,
        boolean autarquiaFederal,
        boolean empresaPublicaFederal,
        boolean state,
        boolean municipal,
        boolean trabalho,
        boolean eleitoral,
        boolean militar,
        boolean publicParty,
        List<String> tags,
        String autorNormalized,
        String reuNormalized
) {
    public String autor() { return autorNormalized; }
    public String reu() { return reuNormalized; }
    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("federal", federal);
        out.put("autarquiaFederal", autarquiaFederal);
        out.put("empresaPublicaFederal", empresaPublicaFederal);
        out.put("state", state);
        out.put("municipal", municipal);
        out.put("trabalho", trabalho);
        out.put("eleitoral", eleitoral);
        out.put("militar", militar);
        out.put("publicParty", publicParty);
        out.put("tags", tags);
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
