package com.tcc.pjb.backend.model.dto.processual.comunicacao.flow;

import jakarta.validation.constraints.NotBlank;

public record NationalCommunicationAcknowledgeRequest(
        @NotBlank String expedicaoUuid,
        @NotBlank String tokenAcuse,
        String ipOrigem,
        String deviceFingerprint,
        String govbrSessionToken) {
}
