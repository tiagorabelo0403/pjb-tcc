package com.tcc.pjb.backend.core.comunicacao.institucional.integration.contract;

import java.util.List;

public record InstitutionalCommunicationContractPayload(
        String jobId,
        String expedicaoUuid,
        Long processoId,
        String processoNumero,
        String unidadeCodigo,
        String caixaCodigo,
        String destinatarioKind,
        String papelProcessual,
        String canal,
        String correlationKey,
        List<String> justificativas
) {
}
