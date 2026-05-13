package com.tcc.pjb.backend.ai.core;

import java.util.Optional;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;

public interface IAService {

    
    IAResponse processar(IARequest request);

    
    IAResponse processar(IAPipelineContext context);

    
    String getTipo();

    
    default IAResponse getUltimaResposta() {
        return null;
    }

    
    default Optional<IAResponse> getUltimaRespostaOptional() {
        return Optional.ofNullable(getUltimaResposta());
    }

}
