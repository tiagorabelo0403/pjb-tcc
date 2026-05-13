package com.tcc.pjb.backend.ai.juridica.mcp;

import com.tcc.pjb.backend.model.dto.ai.legal.eval.LegalEvalReplayResult;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpDeliberationPlan;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpExecutionPlan;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpExecutionTranscript;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpSkillDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.mcp.LegalMcpToolExample;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LegalMcpExecutionTranscriptService {

    public LegalMcpExecutionTranscript capture(LegalMcpServerProfile.ResolveRequest request,
                                               LegalMcpExecutionPlan plan,
                                               LegalEvalReplayResult evaluation,
                                               List<LegalMcpSkillDescriptor> skills,
                                               List<LegalMcpToolExample> examples,
                                               LegalMcpDeliberationPlan deliberation) {
        Objects.requireNonNull(plan, "plan");
        List<String> checkpoints = new ArrayList<>();
        checkpoints.add("PLAN_PINNED_SERVERS");
        if (skills != null && !skills.isEmpty()) {
            checkpoints.add("SKILLS_BOUND");
        }
        if (examples != null && !examples.isEmpty()) {
            checkpoints.add("TOOL_EXAMPLES_BOUND");
        }
        if (evaluation != null) {
            checkpoints.add(evaluation.passed() ? "BENCHMARK_PASS" : "BENCHMARK_REVIEW");
        }
        if (deliberation != null && deliberation.required()) {
            checkpoints.add("DELIBERATION_GATE");
        }
        LinkedHashSet<String> riskFlags = new LinkedHashSet<>();
        if (request != null && request.sigilo()) {
            riskFlags.add("SIGILO");
        }
        if (request != null && request.promptInjectionDetected()) {
            riskFlags.add("PROMPT_INJECTION");
        }
        if (request != null && request.quarantinedContext()) {
            riskFlags.add("QUARANTINED_CONTEXT");
        }
        if (evaluation != null && !evaluation.passed()) {
            riskFlags.add("BENCHMARK_BELOW_TARGET");
        }
        return new LegalMcpExecutionTranscript(
                "LEGAL_MCP_TRANSCRIPT_" + UUID.randomUUID(),
                plan.planId(),
                captureMode(request, deliberation),
                evaluation != null,
                deliberation != null && deliberation.required(),
                plan.pinnedServers() == null ? List.of() : plan.pinnedServers().stream().map(server -> server.serverId()).toList(),
                skills == null ? List.of() : skills.stream().map(LegalMcpSkillDescriptor::skillId).toList(),
                examples == null ? List.of() : examples.stream().map(LegalMcpToolExample::exampleId).toList(),
                List.copyOf(checkpoints),
                List.copyOf(riskFlags),
                evaluation == null ? List.of() : flattenHints(evaluation)
        );
    }

    private String captureMode(LegalMcpServerProfile.ResolveRequest request,
                               LegalMcpDeliberationPlan deliberation) {
        if (request != null && (request.promptInjectionDetected() || request.quarantinedContext())) {
            return "SANITIZED_REPLAY_TRANSCRIPT";
        }
        if (deliberation != null && deliberation.required()) {
            return "APPROVAL_LINKED_TRANSCRIPT";
        }
        return "READONLY_INLINE_TRANSCRIPT";
    }

    private List<String> flattenHints(LegalEvalReplayResult evaluation) {
        if (evaluation == null || evaluation.adaptationHints() == null || evaluation.adaptationHints().isEmpty()) {
            return List.of();
        }
        return evaluation.adaptationHints().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + String.valueOf(entry.getValue()))
                .toList();
    }
}
