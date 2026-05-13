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
public class LegalMcpServerPromotionPolicy {

    public List<String> promote(LegalEvalSuite suite, LegalMcpExecutionPlan plan, List<LegalEvalMetric> metrics, double qualityScore) {
        if (suite == null || plan == null || metrics == null || qualityScore < 85.0d) {
            return List.of();
        }
        LinkedHashSet<String> expected = new LinkedHashSet<>();
        for (LegalEvalCase evalCase : suite.cases()) {
            if (evalCase.requiredPinnedServers() != null) {
                expected.addAll(evalCase.requiredPinnedServers());
            }
        }
        LinkedHashSet<String> promoted = new LinkedHashSet<>();
        for (LegalMcpServerDescriptor server : plan.pinnedServers()) {
            if (expected.contains(server.serverId())) {
                promoted.add(server.serverId());
            }
        }
        return List.copyOf(promoted);
    }
}
