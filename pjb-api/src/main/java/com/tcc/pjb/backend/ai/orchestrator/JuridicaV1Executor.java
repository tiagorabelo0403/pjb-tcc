package com.tcc.pjb.backend.ai.orchestrator;

import java.time.Instant;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.contract.IARequest;
import com.tcc.pjb.backend.ai.contract.IAResponse;

@Component
public class JuridicaV1Executor implements IAExecutor {

    @Override
    public IAResponse executar(IARequest request) {

        return IAResponse.builder()
                .origem("JURIDICA_V1")
                .status(IAResponse.StatusIA.SUCESSO)
                .texto("Análise jurídica preliminar realizada com base nas informações fornecidas.")
                .confianca(0.85)
                .dataGeracao(Instant.now())
                .build();
    }
}
