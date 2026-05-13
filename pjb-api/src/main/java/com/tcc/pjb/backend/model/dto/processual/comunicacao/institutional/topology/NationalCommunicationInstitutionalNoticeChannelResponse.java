package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

public record NationalCommunicationInstitutionalNoticeChannelResponse(
        String canal,
        boolean principalJuridico,
        boolean avisoInformativo,
        String finalidade,
        String observacao
) {
}
