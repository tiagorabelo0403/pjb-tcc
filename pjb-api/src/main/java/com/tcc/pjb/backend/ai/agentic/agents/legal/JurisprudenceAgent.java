package com.tcc.pjb.backend.ai.agentic.agents.legal;

import java.util.*;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.agentic.agents.common.Agent;
import com.tcc.pjb.backend.ai.agentic.core.AgentResult;
import com.tcc.pjb.backend.ai.agentic.core.AgenticRunRequest;
import com.tcc.pjb.backend.ai.juridica.v3.core.LegalJurisprudenceSearchService;
import com.tcc.pjb.backend.core.util.PayloadMaps;

@Component
public class JurisprudenceAgent implements Agent {

    private final LegalJurisprudenceSearchService jurisprudenceSearchService;

    public JurisprudenceAgent(LegalJurisprudenceSearchService jurisprudenceSearchService) {
        this.jurisprudenceSearchService = jurisprudenceSearchService;
    }

    @Override
    public String name() {
        return "JurisprudenceAgent";
    }

    @Override
    public AgentResult execute(AgenticRunRequest request) {
        String query = resolveQuery(request);
        String ramo = resolveFirst(request, "ramoDireito", "ramo_direito", "ramo");
        String ritoName = resolveFirst(request, "rito", "ritoName", "rito_processual", "ritoProcessual");
        List<Map<String, Object>> hits = jurisprudenceSearchService.search(query, ramo, ritoName);

        double confidence = computeConfidence(hits);
        List<String> actions = hits.isEmpty() ? List.of() : List.of("revisar_citacoes", "validar_aderencia_ao_caso");

        AgentResult r = new AgentResult();
        r.setAgent(name());
        r.setConfidence(confidence);
        r.setHumanReviewRequired(false);
        r.setData(PayloadMaps.ofEntries(
                "query", query,
                "ramoDireito", ramo,
                "rito", ritoName,
                "topHits", hits,
                "actions", actions
        ));
        return r;
    }

    private static String resolveQuery(AgenticRunRequest request) {
        if (request == null) return "";
        Map<String, Object> in = request.getInput();
        if (in != null) {
            Object q = in.get("query");
            if (q != null && !String.valueOf(q).isBlank()) return String.valueOf(q).trim();
            q = in.get("pergunta");
            if (q != null && !String.valueOf(q).isBlank()) return String.valueOf(q).trim();
            q = in.get("tema");
            if (q != null && !String.valueOf(q).isBlank()) return String.valueOf(q).trim();
        }
        return request.getTask();
    }

    private static String resolveFirst(AgenticRunRequest request, String... keys) {
        if (request == null || request.getInput() == null || keys == null) return null;
        Map<String, Object> in = request.getInput();
        for (String key : keys) {
            Object value = in.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private static double computeConfidence(List<Map<String, Object>> hits) {
        if (hits == null || hits.isEmpty()) return 0.35;
        double top = 0.0;
        for (int i = 0; i < Math.min(3, hits.size()); i++) {
            Object s = hits.get(i).get("score");
            if (s instanceof Number n) {
                top = Math.max(top, n.doubleValue());
            }
        }
        if (top <= 0.0) return 0.55;
        double scaled = 0.55 + (Math.min(1.0, top / 2.0) * 0.40);
        return Math.max(0.25, Math.min(0.95, scaled));
    }
}
