package com.tcc.pjb.backend.ai.contract;

import com.tcc.pjb.backend.ai.core.model.AgentExecutionContext;

public interface PipelinePhase {

    void execute(AgentExecutionContext ctx);

    default String id() {
        return getClass().getSimpleName();
    }
}
