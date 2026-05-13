package com.tcc.pjb.backend.ai.contract;

import com.tcc.pjb.backend.ai.core.enums.CognitiveStage;
import com.tcc.pjb.backend.ai.core.model.CognitiveContext;

public interface CognitiveAgent {

    CognitiveStage stage();

    void process(CognitiveContext context);

    default String id() {
        return getClass().getSimpleName();
    }
}
