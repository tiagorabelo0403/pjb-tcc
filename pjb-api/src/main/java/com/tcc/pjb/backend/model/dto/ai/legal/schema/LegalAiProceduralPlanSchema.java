package com.tcc.pjb.backend.model.dto.ai.legal.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LegalAiProceduralPlanSchema {

    private LegalAiProceduralPlanSchema() {
    }

    public static LegalAiSchemaDefinition definition() {
        return new LegalAiSchemaDefinition(
                "LEGAL_AI_PROCEDURAL_PLAN",
                "LegalAiProceduralPlanSchema",
                "PROCEDURAL_PLAN",
                List.of("V2", "V3"),
                List.of("ADVOGADO", "SERVIDOR", "MAGISTRADO", "INSTITUCIONAL"),
                List.of("rite", "phase", "risks", "nextSteps"),
                false,
                true,
                List.of(
                        new LegalAiSchemaField("rite", "string", true, "Rito aplicável ao plano.", List.of()),
                        new LegalAiSchemaField("phase", "string", true, "Fase processual identificada.", List.of()),
                        new LegalAiSchemaField("risks", "list<string>", true, "Riscos processuais relevantes.", List.of()),
                        new LegalAiSchemaField("nextSteps", "list<object>", true, "Próximos passos executáveis.", List.of(
                                new LegalAiSchemaField("action", "string", true, "Ação sugerida.", List.of()),
                                new LegalAiSchemaField("owner", "string", false, "Responsável provável.", List.of()),
                                new LegalAiSchemaField("deadline", "string", false, "Marco temporal ou prazo.", List.of())
                        ))
                ),
                example()
        );
    }

    private static Map<String, Object> example() {
        LinkedHashMap<String, Object> step = new LinkedHashMap<>();
        step.put("action", "Protocolar contrarrazões");
        step.put("owner", "ADVOGADO");
        step.put("deadline", "15 dias úteis");
        LinkedHashMap<String, Object> example = new LinkedHashMap<>();
        example.put("rite", "comum");
        example.put("phase", "recursal");
        example.put("risks", List.of("preclusão", "deserção"));
        example.put("nextSteps", List.of(Map.copyOf(step)));
        return Map.copyOf(example);
    }
}
