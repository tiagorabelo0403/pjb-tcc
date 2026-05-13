package com.tcc.pjb.backend.ai.core.pipeline;

import com.tcc.pjb.backend.ai.core.model.AgentExecutionContext;

public interface CognitivePhase {

    CognitivePhaseName name();

    void execute(AgentExecutionContext ctx);
}
