package com.tcc.pjb.backend.ai.orchestrator;

import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;

public interface IAExecutor {

    IAResponse executar(IARequest request);

}
