package com.tcc.pjb.backend.ai.agentic.core;

import java.util.HashMap;
import java.util.Map;

public class AgentResult {
    private String agent;
    private double confidence;
    private boolean humanReviewRequired;
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
