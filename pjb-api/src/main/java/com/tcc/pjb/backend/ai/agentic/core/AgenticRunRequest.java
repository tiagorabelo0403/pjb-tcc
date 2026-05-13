package com.tcc.pjb.backend.ai.agentic.core;

import java.util.HashMap;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AgenticRunRequest {

    private String clientTraceId;

    @NotBlank
    private String task;

    private AgenticDomain domain;

    @NotNull
    private Map<String, Object> input = new HashMap<>();

    @NotNull
    private HumanInLoopPolicy policy = HumanInLoopPolicy.defaultPolicy();

    public String getClientTraceId() {
        return clientTraceId;
    }

    public void setClientTraceId(String clientTraceId) {
        this.clientTraceId = clientTraceId;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public AgenticDomain getDomain() {
        return domain;
    }

    public void setDomain(AgenticDomain domain) {
        this.domain = domain;
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public void setInput(Map<String, Object> input) {
        this.input = input;
    }

    public HumanInLoopPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(HumanInLoopPolicy policy) {
        this.policy = policy;
    }

    @Deprecated(forRemoval = true)
    public Map<String, Object> getPayload() {
        return getInput();
    }

    @Deprecated(forRemoval = true)
    public String getTaskType() {
        return getTask();
    }

    @Deprecated(forRemoval = true)
    public HumanInLoopPolicy getHumanInLoopPolicy() {
        return getPolicy();
    }
}
