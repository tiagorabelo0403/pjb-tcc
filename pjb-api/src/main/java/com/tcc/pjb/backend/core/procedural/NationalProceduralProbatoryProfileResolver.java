package com.tcc.pjb.backend.core.procedural;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralProbatoryProfileResolver {

    String resolve(Map<String, Object> payload, String corpus) {
        Map<String, Object> source = payload == null ? Map.of() : payload;
        String provas = normalize(firstNonBlank(text(source.get("provas")), text(source.get("materialProbatorioResumo")), corpus));
        if (containsAny(provas, "CONTABIL COMPLEXA", "BALANCETE", "PERICIA CONTABIL", "APURACAO DE HAVERES")) {
            return "CONTABIL_COMPLEXA";
        }
        if (containsAny(provas, "ENGENHARIA", "ESTRUTURAL", "INSPECAO TECNICA", "VISTORIA TECNICA")) {
            return "ENGENHARIA_COMPLEXA";
        }
        if (containsAny(provas, "MEDICA COMPLEXA", "LAUDO MEDICO COMPLEXO", "CIRURGIA", "INCAPACIDADE LABORAL", "NEXO CAUSAL MEDICO")) {
            return "MEDICA_COMPLEXA";
        }
        if (containsAny(provas, "CADEIA DOCUMENTAL MASSIVA", "MUITOS CONTRATOS", "CENTENAS DE DOCUMENTOS", "EXTRATOS EM LOTE")) {
            return "CADEIA_DOCUMENTAL_MASSIVA";
        }
        if (containsAny(provas, "PERICIA", "ASSISTENTE TECNICO", "QUESITOS", "PROVA TECNICA")) {
            return "TECNICA_SIMPLIFICADA";
        }
        if (containsAny(provas, "TESTEMUNHA", "DEPOIMENTO", "PROVA ORAL", "OITIVA")) {
            return "TESTEMUNHAL_SIMPLES";
        }
        return "DOCUMENTAL_SIMPLES";
    }

    private static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }

    private static boolean containsAny(String value, String... keys) {
        return NationalProceduralRoutingSupport.containsAny(value, keys);
    }

    private static String normalize(String value) {
        return NationalProceduralRoutingSupport.normalize(value);
    }

    private static String text(Object value) {
        return NationalProceduralRoutingSupport.text(value);
    }
}
