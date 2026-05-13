package com.tcc.pjb.backend.ai.contract;

import com.tcc.pjb.backend.ai.core.enums.IADomain;
import com.tcc.pjb.backend.ai.core.model.CognitiveContext;

public interface DomainAgent {

    IADomain domain();

    void support(CognitiveContext context);

    default String id() {
        return getClass().getSimpleName();
    }
}
