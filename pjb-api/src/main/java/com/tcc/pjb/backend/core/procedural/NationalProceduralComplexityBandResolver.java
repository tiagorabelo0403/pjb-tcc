package com.tcc.pjb.backend.core.procedural;

import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralComplexityBandResolver {

    String resolve(NationalProceduralComplexityContext context) {
        Map<String, Object> payload = context.payload() == null ? Map.of() : context.payload();
        int score = 0;
        if (context.actionProfile().specialProcedure()) {
            score += 1;
        }
        if (context.partyProfile().publicParty()) {
            score += 1;
        }
        if (context.partyProfile().eleitoral() || context.partyProfile().militar()) {
            score += 2;
        }
        if (context.teto().alerta()) {
            score += 1;
        }
        if (context.teto().bloqueante()) {
            score += 3;
        }
        if (context.juizadoDecision().requiresReview()) {
            score += 2;
        }
        if (bool(payload.get("envolveMenor")) || bool(payload.get("envolveSaude")) || bool(payload.get("casoUrgente"))) {
            score += 1;
        }
        score += switch (firstNonBlank(context.probatoryProfile(), "DOCUMENTAL_SIMPLES")) {
            case "TECNICA_SIMPLIFICADA" -> 1;
            case "CONTABIL_COMPLEXA", "ENGENHARIA_COMPLEXA", "MEDICA_COMPLEXA", "CADEIA_DOCUMENTAL_MASSIVA" -> 3;
            default -> 0;
        };
        if (score >= 7) {
            return "CRITICA";
        }
        if (score >= 4) {
            return "ELEVADA";
        }
        if (score >= 2) {
            return "MODERADA";
        }
        return "BAIXA";
    }

    private static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }

    private static boolean bool(Object value) {
        return NationalProceduralRoutingSupport.bool(value);
    }
}
