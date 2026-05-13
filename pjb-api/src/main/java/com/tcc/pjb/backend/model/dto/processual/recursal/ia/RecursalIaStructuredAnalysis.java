package com.tcc.pjb.backend.model.dto.processual.recursal.ia;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record RecursalIaStructuredAnalysis(
        Map<String, Object> contextoCaso,
        Map<String, Object> classificacaoMaterial,
        List<String> riscosAnulacao,
        List<String> checklistBlindagem,
        List<String> fundamentosEstruturais,
        List<String> tesesPrioritarias,
        Map<String, Object> jurisprudencia,
        Map<String, Object> jurimetria,
        Map<String, Object> blueprintRecursal,
        Map<String, Object> memoriaProcessual,
        Map<String, Object> sigiloRecursal,
        Map<String, Object> contrarrazoes,
        Map<String, Object> embargosEspecializados,
        Map<String, Object> assinaturaRecursal,
        Map<String, Object> protocoloExterno,
        Map<String, Object> precedentesQualificados) {

    public RecursalIaStructuredAnalysis {
        contextoCaso = safeMap(contextoCaso);
        classificacaoMaterial = safeMap(classificacaoMaterial);
        riscosAnulacao = riscosAnulacao == null ? List.of() : List.copyOf(riscosAnulacao);
        checklistBlindagem = checklistBlindagem == null ? List.of() : List.copyOf(checklistBlindagem);
        fundamentosEstruturais = fundamentosEstruturais == null ? List.of() : List.copyOf(fundamentosEstruturais);
        tesesPrioritarias = tesesPrioritarias == null ? List.of() : List.copyOf(tesesPrioritarias);
        jurisprudencia = safeMap(jurisprudencia);
        jurimetria = safeMap(jurimetria);
        blueprintRecursal = safeMap(blueprintRecursal);
        memoriaProcessual = safeMap(memoriaProcessual);
        sigiloRecursal = safeMap(sigiloRecursal);
        contrarrazoes = safeMap(contrarrazoes);
        embargosEspecializados = safeMap(embargosEspecializados);
        assinaturaRecursal = safeMap(assinaturaRecursal);
        protocoloExterno = safeMap(protocoloExterno);
        precedentesQualificados = safeMap(precedentesQualificados);
    }

    private static Map<String, Object> safeMap(Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                sanitized.put(key, value);
            }
        });
        return Map.copyOf(sanitized);
    }
}
