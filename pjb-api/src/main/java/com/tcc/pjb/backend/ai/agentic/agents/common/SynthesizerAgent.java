package com.tcc.pjb.backend.ai.agentic.agents.common;

import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.agentic.core.AgentResult;
import com.tcc.pjb.backend.ai.agentic.core.AgenticRoutingDecision;
import com.tcc.pjb.backend.ai.agentic.core.AgenticRunRequest;

@Component
public class SynthesizerAgent {

    public record SynthesisResult(Map<String, Object> report, Map<String, Object> explainability) {
    }

    public SynthesisResult synthesize(AgenticRoutingDecision decision, AgenticRunRequest request, List<AgentResult> results) {
        Map<String, Object> report = new LinkedHashMap<>();
        Map<String, Object> explainability = new LinkedHashMap<>();

        String task = request != null ? request.getTask() : "";

        report.put("task", task);
        report.put("domain", decision != null ? decision.domain().name() : "LEGAL");
        report.put("materia", decision != null && decision.materia() != null ? decision.materia().materia().name() : "MULTIMATERIA");
        report.put("generatedAt", Instant.now().toString());

        List<Map<String, Object>> agentSummaries = new ArrayList<>();
        if (results != null) {
            for (AgentResult r : results) {
                agentSummaries.add(summarize(r));
            }
        }

        report.put("agents", agentSummaries);

        Map<String, Object> sigilo = new LinkedHashMap<>();
        if (decision != null && decision.sigilo() != null) {
            sigilo.put("nivel", decision.sigilo().nivel().name());
            sigilo.put("score", decision.sigilo().score());
            sigilo.put("signals", decision.sigilo().signals().stream().map(Enum::name).toList());
            sigilo.put("recomendacoes", decision.sigilo().recomendacoes());
        }
        report.put("sigilo", sigilo);

        explainability.put("routing", Map.of(
                "effectiveQuery", decision != null ? decision.effectiveQuery() : task,
                "materiaSignals", decision != null && decision.materia() != null ? decision.materia().signals() : List.of(),
                "materiaConfidence", decision != null && decision.materia() != null ? decision.materia().confidence() : 0.0
        ));

        explainability.put("agentConfidence", confidenceMap(results));

        return new SynthesisResult(report, explainability);
    }

    private static Map<String, Object> summarize(AgentResult r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("agent", r.getAgent());
        m.put("confidence", r.getConfidence());
        m.put("humanReviewRequired", r.isHumanReviewRequired());
        if (r.getData() != null && !r.getData().isEmpty()) {
            m.put("data", r.getData());
        }
        return m;
    }

    private static Map<String, Double> confidenceMap(List<AgentResult> results) {
        if (results == null) return Map.of();
        LinkedHashMap<String, Double> out = new LinkedHashMap<>();
        for (AgentResult r : results) {
            String k = r.getAgent() == null ? "unknown" : r.getAgent();
            out.put(k, r.getConfidence());
        }
        return out;
    }
}
