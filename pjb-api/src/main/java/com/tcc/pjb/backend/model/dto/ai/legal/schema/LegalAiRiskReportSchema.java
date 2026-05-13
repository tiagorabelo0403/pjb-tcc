package com.tcc.pjb.backend.model.dto.ai.legal.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LegalAiRiskReportSchema {

    private LegalAiRiskReportSchema() {
    }

    public static LegalAiSchemaDefinition definition() {
        return new LegalAiSchemaDefinition(
                "LEGAL_AI_RISK_REPORT_SCHEMA",
                "LegalAiRiskReportSchema",
                "RISK_REPORT",
                List.of("V2", "V3"),
                List.of("ADVOGADO", "MAGISTRADO", "INSTITUCIONAL"),
                List.of("riskLevel", "riskVectors", "mitigations", "blockingDependencies"),
                false,
                true,
                List.of(
                        new LegalAiSchemaField("riskLevel", "string", true, "Nível agregado de risco.", List.of()),
                        new LegalAiSchemaField("riskVectors", "list<object>", true, "Vetores de risco jurídico-operacional.", List.of(
                                new LegalAiSchemaField("name", "string", true, "Nome do vetor de risco.", List.of()),
                                new LegalAiSchemaField("impact", "string", true, "Impacto provável.", List.of()),
                                new LegalAiSchemaField("likelihood", "string", true, "Probabilidade.", List.of())
                        )),
                        new LegalAiSchemaField("mitigations", "list<string>", true, "Medidas mitigatórias.", List.of()),
                        new LegalAiSchemaField("blockingDependencies", "list<string>", true, "Dependências que bloqueiam mitigação total.", List.of())
                ),
                example()
        );
    }

    private static Map<String, Object> example() {
        LinkedHashMap<String, Object> vector = new LinkedHashMap<>();
        vector.put("name", "tempestividade não confirmada");
        vector.put("impact", "alto");
        vector.put("likelihood", "média");
        LinkedHashMap<String, Object> example = new LinkedHashMap<>();
        example.put("riskLevel", "ALTO");
        example.put("riskVectors", List.of(Map.copyOf(vector)));
        example.put("mitigations", List.of("validar intimação", "recalcular prazo"));
        example.put("blockingDependencies", List.of("certidão de intimação"));
        return Map.copyOf(example);
    }
}
