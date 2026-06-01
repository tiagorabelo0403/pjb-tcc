package com.tcc.pjb.backend.ai.agentic.core;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.ai.explainability.ExplanationBundle;

public class AgenticRunResponse {

    private String traceId;
    private Instant createdAt = Instant.now();
    private String summary;
    @Schema(hidden = true)
    private Map<String, Object> output = new HashMap<>();
    private List<AgentResult> agentResults = new ArrayList<>();
    private List<ApprovalItem> approvalsRequired = new ArrayList<>();
    private ExplanationBundle explanation;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    @JsonIgnore
    public Map<String, Object> getOutput() {
        return output;
    }

    public void setOutput(Map<String, Object> output) {
        this.output = output;
    }

    public List<AgentResult> getAgentResults() {
        return agentResults;
    }

    public void setAgentResults(List<AgentResult> agentResults) {
        this.agentResults = agentResults;
    }

    public List<ApprovalItem> getApprovalsRequired() {
        return approvalsRequired;
    }

    public void setApprovalsRequired(List<ApprovalItem> approvalsRequired) {
        this.approvalsRequired = approvalsRequired;
    }

    public ExplanationBundle getExplanation() {
        return explanation;
    }

    public void setExplanation(ExplanationBundle explanation) {
        this.explanation = explanation;
    }

    @Deprecated(forRemoval = true)
    public void setDomain(AgenticDomain domain) {
        if (this.output == null) this.output = new HashMap<>();
        this.output.put("domain", domain == null ? null : domain.name());
    }

    @Deprecated(forRemoval = true)
    public void setTaskType(String taskType) {
        if (this.output == null) this.output = new HashMap<>();
        this.output.put("taskType", taskType);
    }

    @Deprecated(forRemoval = true)
    public void setReport(String report) {
        if (this.output == null) this.output = new HashMap<>();
        this.output.put("report", report);
    }

    @Deprecated(forRemoval = true)
    public void setExplainability(ExplanationBundle bundle) {
        setExplanation(bundle);
    }

    @Deprecated(forRemoval = true)
    public ExplanationBundle getExplainability() {
        return getExplanation();
    }
}
