package com.tcc.pjb.backend.model.dto.ai.legal.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LegalAiDecisaoSchema {

    private LegalAiDecisaoSchema() {
    }

    public static LegalAiSchemaDefinition definition() {
        return new LegalAiSchemaDefinition(
                "LEGAL_AI_DECISAO_SCHEMA",
                "LegalAiDecisaoSchema",
                "DECISAO",
                List.of("V3"),
                List.of("MAGISTRADO"),
                List.of("report", "controvertedPoints", "grounds", "device", "effects"),
                true,
                true,
                List.of(
                        new LegalAiSchemaField("report", "string", true, "Relatório da decisão.", List.of()),
                        new LegalAiSchemaField("controvertedPoints", "list<string>", true, "Pontos controvertidos enfrentados.", List.of()),
                        new LegalAiSchemaField("grounds", "list<object>", true, "Fundamentos da decisão.", List.of(
                                new LegalAiSchemaField("authority", "string", true, "Autoridade aplicável.", List.of()),
                                new LegalAiSchemaField("reasoning", "string", true, "Raciocínio decisório.", List.of())
                        )),
                        new LegalAiSchemaField("device", "string", true, "Dispositivo da decisão.", List.of()),
                        new LegalAiSchemaField("effects", "list<string>", true, "Efeitos processuais imediatos.", List.of())
                ),
                example()
        );
    }

    private static Map<String, Object> example() {
        LinkedHashMap<String, Object> ground = new LinkedHashMap<>();
        ground.put("authority", "precedente [NAO_CONFIRMADO]");
        ground.put("reasoning", "necessária conferência de aderência ao caso concreto");
        LinkedHashMap<String, Object> example = new LinkedHashMap<>();
        example.put("report", "É o relatório. Decido.");
        example.put("controvertedPoints", List.of("cabimento", "tempestividade"));
        example.put("grounds", List.of(Map.copyOf(ground)));
        example.put("device", "Defiro parcialmente o pedido, nos limites do grounding confirmado.");
        example.put("effects", List.of("intimação das partes", "remessa à secretaria"));
        return Map.copyOf(example);
    }
}
