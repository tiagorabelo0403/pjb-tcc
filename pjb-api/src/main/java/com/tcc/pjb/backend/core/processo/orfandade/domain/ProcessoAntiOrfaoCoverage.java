package com.tcc.pjb.backend.core.processo.orfandade.domain;

import java.util.List;

public record ProcessoAntiOrfaoCoverage(
        String eixo,
        String aggregateClass,
        String applicationServiceClass,
        String endpointPath,
        String testReference,
        String diagnosticReference,
        String status,
        boolean connected,
        List<String> fundamentos
) {
    public ProcessoAntiOrfaoCoverage {
        eixo = eixo == null || eixo.isBlank() ? "DESCONHECIDO" : eixo;
        aggregateClass = aggregateClass == null ? "" : aggregateClass;
        applicationServiceClass = applicationServiceClass == null ? "" : applicationServiceClass;
        endpointPath = endpointPath == null ? "" : endpointPath;
        testReference = testReference == null ? "" : testReference;
        diagnosticReference = diagnosticReference == null ? "" : diagnosticReference;
        status = status == null || status.isBlank() ? (connected ? "CONNECTED" : "PARTIAL") : status;
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
