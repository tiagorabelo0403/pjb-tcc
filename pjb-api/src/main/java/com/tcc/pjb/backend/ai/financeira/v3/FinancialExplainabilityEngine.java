package com.tcc.pjb.backend.ai.financeira.v3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;

@Component
public class FinancialExplainabilityEngine {

    public FinancialExplainability explain(IARequest req, IAResponse base) {
        List<String> rationales = new ArrayList<>();
        List<String> levers = new ArrayList<>();

        String ramo = safeStr(base, "ramo_direito");
        Object vc = safeObj(base, "valor_causa");

        rationales.add("A estimativa é determinística e retorna *ranges* (não valores exatos).");
        if (ramo != null) rationales.add("Ramo inferido: " + ramo + " (influencia overhead e risco).");
        if (vc != null) rationales.add("Valor da causa influencia proporcionalmente custas/provisão.");
        if (vc == null) rationales.add("Sem valor da causa: usando base conservadora para não subestimar.");

        levers.add("valorCausa: aumenta range e risco proporcionalmente");
        levers.add("faseProcessual: altera probabilidade (P) e timing de desembolso");
        levers.add("perícia/contábil: aumenta overhead e variância");
        levers.add("tutela/depósito/garantia: muda custo de oportunidade e caixa");

        Map<String, Object> sensitivity = sensitivitySkeleton(req);

        return new FinancialExplainability(rationales, levers, sensitivity);
    }

    private static Map<String, Object> sensitivitySkeleton(IARequest req) {
        Map<String, Object> s = new LinkedHashMap<>();
        
        s.put("dimensions", List.of(
                Map.of("name", "valorCausa", "hint", "Informe valor numérico para refinar ranges"),
                Map.of("name", "faseProcessual", "hint", "Ex.: inicial, tutela, sentença, recurso"),
                Map.of("name", "pericia", "hint", "true/false: se há perícia/contábil"),
                Map.of("name", "garantia", "hint", "Ex.: depósito, seguro, fiança")
        ));
        s.put("inputs_detected", req != null ? req.getPayload().keySet() : List.of());
        return s;
    }

    private static String safeStr(IAResponse base, String key) {
        Object v = safeObj(base, key);
        return v != null ? String.valueOf(v) : null;
    }

    private static Object safeObj(IAResponse base, String key) {
        if (base == null || base.getMetadados() == null || key == null) return null;
        return base.getMetadados().get(key);
    }

    public record FinancialExplainability(
            List<String> rationales,
            List<String> levers,
            Map<String, Object> sensitivity
    ) {
        public FinancialExplainability {
            rationales = rationales == null ? List.of() : List.copyOf(rationales);
            levers = levers == null ? List.of() : List.copyOf(levers);
            sensitivity = sensitivity == null ? Map.of() : Map.copyOf(sensitivity);
        }

        public String humanReadable() {
            StringBuilder sb = new StringBuilder();
            if (!rationales.isEmpty()) {
                sb.append("Racional:\n");
                for (String r : rationales) {
                    sb.append("- ").append(r).append("\n");
                }
            }
            if (!levers.isEmpty()) {
                sb.append("\nPrincipais alavancas:\n");
                for (String l : levers) {
                    sb.append("- ").append(l).append("\n");
                }
            }
            sb.append("\nSensibilidade: estrutura entregue em metadados (para UI).\n");
            return sb.toString();
        }

        @Override
        public String toString() {
            return "FinancialExplainability{rationales=" + rationales.size() + ", levers=" + levers.size() + ", sensitivityKeys="
                    + (sensitivity != null ? sensitivity.keySet() : List.of()) + "}";
        }
    }
}
