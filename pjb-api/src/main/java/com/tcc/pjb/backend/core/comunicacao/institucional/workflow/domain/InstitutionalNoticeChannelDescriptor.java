package com.tcc.pjb.backend.core.comunicacao.institucional.workflow.domain;

public record InstitutionalNoticeChannelDescriptor(
        String canal,
        boolean principalJuridico,
        boolean avisoInformativo,
        String finalidade,
        String observacao
) {
}
