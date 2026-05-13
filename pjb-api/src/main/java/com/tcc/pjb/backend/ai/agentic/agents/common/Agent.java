package com.tcc.pjb.backend.ai.agentic.agents.common;

import com.tcc.pjb.backend.ai.agentic.core.AgentResult;
import com.tcc.pjb.backend.ai.agentic.core.AgenticRunRequest;

public interface Agent {
    String name();

    
    AgentResult execute(AgenticRunRequest request);
}
