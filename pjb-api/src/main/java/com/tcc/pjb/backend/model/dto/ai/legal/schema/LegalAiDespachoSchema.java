package com.tcc.pjb.backend.model.dto.ai.legal.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LegalAiDespachoSchema {

    private LegalAiDespachoSchema() {
    }

    public static LegalAiSchemaDefinition definition() {
        return new LegalAiSchemaDefinition(
                "LEGAL_AI_DESPACHO_SCHEMA",
                "LegalAiDespachoSchema",
                "DESPACHO",
                List.of("V3"),
                List.of("MAGISTRADO", "SERVIDOR"),
                List.of("report", "grounds", "order", "executionDesk"),
                true,
                true,
                List.of(
                        new LegalAiSchemaField("report", "string", true, "Relatório mínimo do despacho.", List.of()),
                        new LegalAiSchemaField("grounds", "list<object>", true, "Fundamentação compatível com o ato ordinatório ou judicial.", List.of(
                                new LegalAiSchemaField("authority", "string", true, "Base jurídica confirmada ou marcada para grounding.", List.of()),
                                new LegalAiSchemaField("reasoning", "string", true, "Razão aplicada ao despacho.", List.of())
                        )),
                        new LegalAiSchemaField("order", "string", true, "Comando do despacho.", List.of()),
                        new LegalAiSchemaField("executionDesk", "string", true, "Painel ou mesa operacional destinatária.", List.of())
                ),
                example()
        );
    }

    private static Map<String, Object> example() {
        LinkedHashMap<String, Object> ground = new LinkedHashMap<>();
        ground.put("authority", "art. [NAO_CONFIRMADO]");
        ground.put("reasoning", "necessidade de saneamento prévio");
        LinkedHashMap<String, Object> example = new LinkedHashMap<>();
        example.put("report", "Vistos. Necessário regularizar a instrução mínima.");
        example.put("grounds", List.of(Map.copyOf(ground)));
        example.put("order", "Intime-se a parte para complementar a documentação em prazo legal.");
        example.put("executionDesk", "SECRETARIA_PROCESSUAL");
        return Map.copyOf(example);
    }
}
