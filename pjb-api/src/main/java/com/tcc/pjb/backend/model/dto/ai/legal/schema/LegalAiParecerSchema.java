package com.tcc.pjb.backend.model.dto.ai.legal.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LegalAiParecerSchema {

    private LegalAiParecerSchema() {
    }

    public static LegalAiSchemaDefinition definition() {
        return new LegalAiSchemaDefinition(
                "LEGAL_AI_PARECER_SCHEMA",
                "LegalAiParecerSchema",
                "PARECER",
                List.of("V2", "V3"),
                List.of("ADVOGADO", "MAGISTRADO", "INSTITUCIONAL"),
                List.of("questionPresented", "relevantFacts", "grounds", "conclusion"),
                true,
                true,
                List.of(
                        new LegalAiSchemaField("questionPresented", "string", true, "Questão jurídica submetida à análise.", List.of()),
                        new LegalAiSchemaField("relevantFacts", "list<string>", true, "Fatos relevantes delimitados.", List.of()),
                        new LegalAiSchemaField("grounds", "list<object>", true, "Fundamentos normativos e jurisprudenciais.", List.of(
                                new LegalAiSchemaField("authority", "string", true, "Autoridade invocada.", List.of()),
                                new LegalAiSchemaField("application", "string", true, "Aplicação ao caso concreto.", List.of())
                        )),
                        new LegalAiSchemaField("conclusion", "string", true, "Conclusão do parecer.", List.of())
                ),
                example()
        );
    }

    private static Map<String, Object> example() {
        LinkedHashMap<String, Object> ground = new LinkedHashMap<>();
        ground.put("authority", "art. [NAO_CONFIRMADO]");
        ground.put("application", "exige confirmação de vigência antes de conclusão final");
        LinkedHashMap<String, Object> example = new LinkedHashMap<>();
        example.put("questionPresented", "Cabimento recursal em rito especial");
        example.put("relevantFacts", List.of("decisão interlocutória", "prazo em curso"));
        example.put("grounds", List.of(Map.copyOf(ground)));
        example.put("conclusion", "Conclusão condicionada ao grounding das autoridades citadas.");
        return Map.copyOf(example);
    }
}
