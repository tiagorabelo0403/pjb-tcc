package com.tcc.pjb.backend.core.procedural;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record NationalProceduralDistributionSuggestion(
        String unidadeCodigo,
        String tribunalCodigo,
        String comarca,
        String uf,
        String tipoVara,
        double scoreFinal,
        String motivacao,
        List<String> alertas,
        List<String> fatoresRevisao
) {
    NationalProceduralDistributionSuggestion(
            java.time.Instant resolvedAt,
            String unidadeCodigo,
            String tribunalCodigo,
            String comarca,
            String uf,
            String tipoVara,
            double scoreFinal,
            boolean revisaoHumana,
            String motivacao,
            List<String> alertas,
            List<String> fatoresRevisao,
            List<String> fundamentos,
            Map<String, ?> metadata
    ) {
        this(unidadeCodigo, tribunalCodigo, comarca, uf, tipoVara, scoreFinal, motivacao, alertas, fatoresRevisao);
    }

    Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("unidadeCodigo", unidadeCodigo);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("comarca", comarca);
        out.put("uf", uf);
        out.put("tipoVara", tipoVara);
        out.put("scoreFinal", scoreFinal);
        out.put("motivacao", motivacao);
        out.put("alertas", alertas == null ? List.of() : List.copyOf(alertas));
        out.put("fatoresRevisao", fatoresRevisao == null ? List.of() : List.copyOf(fatoresRevisao));
        out.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return Collections.unmodifiableMap(out);
    }
}
