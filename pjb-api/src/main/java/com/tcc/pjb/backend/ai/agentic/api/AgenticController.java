package com.tcc.pjb.backend.ai.agentic.api;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.ai.agentic.core.AgentResult;
import com.tcc.pjb.backend.ai.agentic.core.AgenticOrchestrator;
import com.tcc.pjb.backend.ai.agentic.core.AgenticRunRequest;
import com.tcc.pjb.backend.ai.agentic.core.AgenticRunResponse;
import com.tcc.pjb.backend.ai.agentic.core.ApprovalItem;

@RestController
@RequestMapping(path = "/api/ai/agentic", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@PreAuthorize("isAuthenticated()")
public class AgenticController {

    private final AgenticOrchestrator orchestrator;

    public AgenticController(AgenticOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping(value = "/run", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AgenticRunResponse run(@Valid @RequestBody AgenticRunRequest request) {
        AgentResult result = orchestrator.run(request);
        AgenticRunResponse response = new AgenticRunResponse();
        response.setTraceId(resolveTraceId(request, result));
        response.setCreatedAt(Instant.now());
        response.setSummary(resolveSummary(request, result));
        response.setOutput(copyOutput(result));
        response.setAgentResults(List.of(result));
        response.setApprovalsRequired(resolveApprovals(result));
        return response;
    }

    private String resolveTraceId(AgenticRunRequest request, AgentResult result) {
        if (request != null && request.getClientTraceId() != null && !request.getClientTraceId().isBlank()) {
            return request.getClientTraceId();
        }
        Object traceId = result != null && result.getData() != null ? result.getData().get("traceId") : null;
        if (traceId != null && !String.valueOf(traceId).isBlank()) {
            return String.valueOf(traceId);
        }
        return null;
    }

    private String resolveSummary(AgenticRunRequest request, AgentResult result) {
        if (result != null && result.getData() != null) {
            Object report = result.getData().get("report");
            if (report != null && !String.valueOf(report).isBlank()) {
                return String.valueOf(report);
            }
        }
        if (result != null && result.getAgent() != null && !result.getAgent().isBlank()) {
            return "Execução agentic concluída por " + result.getAgent() + ".";
        }
        return request != null && request.getTask() != null && !request.getTask().isBlank()
                ? "Execução agentic concluída para a tarefa " + request.getTask() + "."
                : "Execução agentic concluída.";
    }

    private Map<String, Object> copyOutput(AgentResult result) {
        if (result == null || result.getData() == null || result.getData().isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, Object> output = new LinkedHashMap<>(result.getData());
        output.putIfAbsent("agent", result.getAgent());
        output.putIfAbsent("confidence", result.getConfidence());
        output.putIfAbsent("humanReviewRequired", result.isHumanReviewRequired());
        return Map.copyOf(output);
    }

    private List<ApprovalItem> resolveApprovals(AgentResult result) {
        if (result == null || !result.isHumanReviewRequired()) {
            return List.of();
        }
        ApprovalItem approval = new ApprovalItem();
        approval.setActionType("HUMAN_REVIEW");
        approval.setDescription("Resultado agentic requer validação humana antes de ação sensível.");
        approval.setConfidence(result.getConfidence());
        return List.of(approval);
    }
}
