package com.tcc.pjb.backend.ai.agentic.core;

public class ApprovalItem {
    private String actionType;
    private String description;
    private double confidence;

    public ApprovalItem() {
    }

    public ApprovalItem(String actionType, String description, double confidence) {
        this.actionType = actionType;
        this.description = description;
        this.confidence = confidence;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }
}
