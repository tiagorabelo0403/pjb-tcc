package com.tcc.pjb.backend.ai.juridica.eval;

import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalCase;
import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalMetric;
import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalSuite;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpExecutionPlan;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpServerDescriptor;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LegalMcpServerDemotionPolicy {

    public List<String> demote(LegalEvalSuite suite, LegalMcpExecutionPlan plan, List<LegalEvalMetric> metrics, double qualityScore) {
        if (suite == null || plan == null) {
            return List.of();
        }
        LinkedHashSet<String> expected = new LinkedHashSet<>();
        for (LegalEvalCase evalCase : suite.cases()) {
            if (evalCase.requiredPinnedServers() != null) {
                expected.addAll(evalCase.requiredPinnedServers());
            }
        }
        LinkedHashSet<String> demoted = new LinkedHashSet<>();
        boolean failed = metrics != null && metrics.stream().anyMatch(metric -> metric.score() < metric.threshold());
        boolean strictInjectionFence = suite.scope() != null && suite.scope().contains("INJECTION");
        for (LegalMcpServerDescriptor server : plan.pinnedServers()) {
            if (strictInjectionFence && !server.serverId().equals("MCP_DOCUMENTAL") && !server.serverId().equals("MCP_PROCESSUAL")) {
                demoted.add(server.serverId());
                continue;
            }
            if ((failed || qualityScore < 70.0d) && !expected.isEmpty() && !expected.contains(server.serverId())) {
                demoted.add(server.serverId());
            }
        }
        return List.copyOf(demoted);
    }
}
