package com.tcc.pjb.backend.model.dto.ai.legal.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LegalAiChecklistSchema {

    private LegalAiChecklistSchema() {
    }

    public static LegalAiSchemaDefinition definition() {
        return new LegalAiSchemaDefinition(
                "LEGAL_AI_CHECKLIST_SCHEMA",
                "LegalAiChecklistSchema",
                "CHECKLIST",
                List.of("V1", "V2", "V3"),
                List.of("ADVOGADO", "SERVIDOR", "MAGISTRADO", "INSTITUCIONAL"),
                List.of("objective", "items", "blockingPoints", "completionCriteria"),
                false,
                true,
                List.of(
                        new LegalAiSchemaField("objective", "string", true, "Objetivo operacional do checklist.", List.of()),
                        new LegalAiSchemaField("items", "list<object>", true, "Itens verificáveis do fluxo jurídico.", List.of(
                                new LegalAiSchemaField("title", "string", true, "Nome do item.", List.of()),
                                new LegalAiSchemaField("status", "string", true, "Status do item.", List.of()),
                                new LegalAiSchemaField("evidence", "string", false, "Evidência vinculada ao item.", List.of())
                        )),
                        new LegalAiSchemaField("blockingPoints", "list<string>", true, "Pontos impeditivos de avanço.", List.of()),
                        new LegalAiSchemaField("completionCriteria", "list<string>", true, "Condições mínimas para conclusão.", List.of())
                ),
                example()
        );
    }

    private static Map<String, Object> example() {
        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
        item.put("title", "Conferir tempestividade");
        item.put("status", "PENDENTE");
        item.put("evidence", "Data da intimação não informada");
        LinkedHashMap<String, Object> example = new LinkedHashMap<>();
        example.put("objective", "Fechar admissibilidade recursal");
        example.put("items", List.of(Map.copyOf(item)));
        example.put("blockingPoints", List.of("Ausência da certidão de intimação"));
        example.put("completionCriteria", List.of("Tempestividade confirmada", "Preparo validado"));
        return Map.copyOf(example);
    }
}
