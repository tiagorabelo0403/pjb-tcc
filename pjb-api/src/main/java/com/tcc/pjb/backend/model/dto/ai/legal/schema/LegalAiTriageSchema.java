package com.tcc.pjb.backend.model.dto.ai.legal.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LegalAiTriageSchema {

    private LegalAiTriageSchema() {
    }

    public static LegalAiSchemaDefinition definition() {
        return new LegalAiSchemaDefinition(
                "LEGAL_AI_TRIAGE_RESULT",
                "LegalAiTriageSchema",
                "TRIAGE",
                List.of("V1", "V2", "V3"),
                List.of("CIDADAO", "ADVOGADO", "SERVIDOR", "MAGISTRADO", "INSTITUCIONAL"),
                List.of("classification", "keywords", "missingEvidence"),
                false,
                false,
                List.of(
                        new LegalAiSchemaField("classification", "string", true, "Classificação material inicial do pedido.", List.of()),
                        new LegalAiSchemaField("keywords", "list<string>", true, "Palavras-chave canônicas para retrieval e roteamento.", List.of()),
                        new LegalAiSchemaField("missingEvidence", "list<string>", true, "Fatos ou documentos ainda ausentes.", List.of())
                ),
                example()
        );
    }

    private static Map<String, Object> example() {
        LinkedHashMap<String, Object> example = new LinkedHashMap<>();
        example.put("classification", "triagem recursal cível");
        example.put("keywords", List.of("apelacao", "tempestividade", "efeito suspensivo"));
        example.put("missingEvidence", List.of("cópia da decisão recorrida", "data da intimação"));
        return Map.copyOf(example);
    }
}
