package com.tcc.pjb.backend.adapter.strategies;

import java.util.Map;
import com.tcc.pjb.backend.shared.dto.PJeAndamentoResponse;
import com.tcc.pjb.backend.shared.dto.PJeAutenticacaoResponse;
import com.tcc.pjb.backend.shared.dto.PJeSubmissaoResponse;

public interface IPJeAdapter {

    
    String getAdapterKey();

    
    PJeAutenticacaoResponse autenticar(Map<String, Object> orgaoConfig,
                                       String correlationId);

    
    PJeSubmissaoResponse submeterProcesso(String authToken,
                                          Map<String, Object> processoVariables,
                                          String correlationId);

    
    PJeAndamentoResponse consultarAndamento(String authToken,
                                            String numeroProcessoTribunal,
                                            String correlationId);

}
