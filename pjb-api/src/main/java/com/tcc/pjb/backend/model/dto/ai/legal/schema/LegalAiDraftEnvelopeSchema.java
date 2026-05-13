package com.tcc.pjb.backend.model.dto.ai.legal.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LegalAiDraftEnvelopeSchema {

    private LegalAiDraftEnvelopeSchema() {
    }

    public static LegalAiSchemaDefinition definition() {
        return new LegalAiSchemaDefinition(
                "LEGAL_AI_DRAFT_ENVELOPE",
                "LegalAiDraftEnvelopeSchema",
                "DRAFT",
                List.of("V3"),
                List.of("ADVOGADO", "MAGISTRADO", "SERVIDOR", "INSTITUCIONAL"),
                List.of("report", "grounds", "device", "providence"),
                true,
                true,
                List.of(
                        new LegalAiSchemaField("report", "string", true, "Relatório sintético do ato.", List.of()),
                        new LegalAiSchemaField("grounds", "list<object>", true, "Fundamentos estruturados.", List.of(
                                new LegalAiSchemaField("authority", "string", true, "Autoridade jurídica vinculada.", List.of()),
                                new LegalAiSchemaField("reasoning", "string", true, "Razão aplicada ao caso.", List.of())
                        )),
                        new LegalAiSchemaField("device", "string", true, "Parte dispositiva.", List.of()),
                        new LegalAiSchemaField("providence", "list<string>", true, "Providências operacionais subsequentes.", List.of())
                ),
                example()
        );
    }

    private static Map<String, Object> example() {
        LinkedHashMap<String, Object> ground = new LinkedHashMap<>();
        ground.put("authority", "precedente [NAO_CONFIRMADO]");
        ground.put("reasoning", "aplicação condicionada à confirmação do grounding");
        LinkedHashMap<String, Object> example = new LinkedHashMap<>();
        example.put("report", "Trata-se de minuta assistida para análise jurídica controlada.");
        example.put("grounds", List.of(Map.copyOf(ground)));
        example.put("device", "[PENDENTE_DELIBERACAO]");
        example.put("providence", List.of("submeter a revisão humana", "confirmar citações"));
        return Map.copyOf(example);
    }
}
