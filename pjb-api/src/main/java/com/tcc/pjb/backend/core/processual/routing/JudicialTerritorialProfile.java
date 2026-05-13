package com.tcc.pjb.backend.core.processual.routing;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record JudicialTerritorialProfile(
        String tribunalCodigo,
        String tribunalNome,
        String cidadeAnchor,
        String comarca,
        String foro,
        String secaoJudiciaria,
        String subsecaoJudiciaria,
        String circunscricao,
        String primeiraInstanciaLabel,
        String segundaInstanciaLabel,
        String superiorLabel,
        String unidadeBase,
        String territorialRegistry,
        boolean specialTerritorialReview,
        List<String> warnings,
        LinkedHashMap<String, Object> metadata) {

    public JudicialTerritorialProfile {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        metadata = metadata == null ? new LinkedHashMap<>() : new LinkedHashMap<>(metadata);
    }

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(metadata);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("tribunalNome", tribunalNome);
        out.put("cidadeAnchor", cidadeAnchor);
        out.put("comarca", comarca);
        out.put("foro", foro);
        out.put("secaoJudiciaria", secaoJudiciaria);
        out.put("subsecaoJudiciaria", subsecaoJudiciaria);
        out.put("circunscricao", circunscricao);
        out.put("primeiraInstanciaLabel", primeiraInstanciaLabel);
        out.put("segundaInstanciaLabel", segundaInstanciaLabel);
        out.put("superiorLabel", superiorLabel);
        out.put("unidadeBase", unidadeBase);
        out.put("territorialRegistry", territorialRegistry);
        out.put("specialTerritorialReview", specialTerritorialReview);
        out.put("warnings", warnings);
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
