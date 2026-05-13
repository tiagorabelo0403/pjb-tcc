package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.panel;

public record NationalCommunicationInstitutionalAnalyticsBucketResponse(
        String dimensao,
        String valor,
        long total,
        double percentual,
        double mediaHoras
) {
}
