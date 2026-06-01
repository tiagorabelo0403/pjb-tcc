package com.tcc.pjb.backend.ai.agentic.core;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;

public class AgentResult {
    private String agent;
    private double confidence;
    private boolean humanReviewRequired;
    @Schema(description = "Payload do agente — estrutura varia por tipo de agente concreto (CashFlow, FinancialRatios, Compliance, Tax, Contract, Jurisprudence)")
    @Size(max = 50)
    private Map<String, Object> data = new HashMap<>();

    public AgentResult() {
    }

    public AgentResult(String agent, double confidence) {
        this.agent = agent;
        this.confidence = confidence;
    }

    public AgentResult(String agent, double confidence, boolean humanReviewRequired) {
        this.agent = agent;
        this.confidence = confidence;
        this.humanReviewRequired = humanReviewRequired;
    }

    public String getAgent() {
        return agent;
    }

    public void setAgent(String agent) {
        this.agent = agent;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public boolean isHumanReviewRequired() {
        return humanReviewRequired;
    }

    public void setHumanReviewRequired(boolean humanReviewRequired) {
        this.humanReviewRequired = humanReviewRequired;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data == null ? new HashMap<>() : new HashMap<>(data);
    }
}
