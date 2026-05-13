package com.tcc.pjb.backend.core.comunicacao.institucional.integration.domain;

import java.util.Objects;
import com.tcc.pjb.backend.model.entity.enums.StatusIntegracaoInstitucionalExterna;

public record InstitutionalExternalDispatchResult(
        StatusIntegracaoInstitucionalExterna status,
        String providerReference,
        String providerStatus,
        String responsePayload,
        String failureReason,
        boolean transientFailure
) {
    public InstitutionalExternalDispatchResult {
        Objects.requireNonNull(status, "status");
    }

    public static InstitutionalExternalDispatchResult accepted(String providerReference, String providerStatus, String responsePayload) {
        return new InstitutionalExternalDispatchResult(StatusIntegracaoInstitucionalExterna.ACEITA, providerReference, providerStatus, responsePayload, null, false);
    }

    public static InstitutionalExternalDispatchResult transientFailure(String providerStatus, String failureReason, String responsePayload) {
        return new InstitutionalExternalDispatchResult(StatusIntegracaoInstitucionalExterna.FALHA_TRANSITORIA, null, providerStatus, responsePayload, failureReason, true);
    }

    public static InstitutionalExternalDispatchResult terminalFailure(String providerStatus, String failureReason, String responsePayload) {
        return new InstitutionalExternalDispatchResult(StatusIntegracaoInstitucionalExterna.FALHA_TERMINAL, null, providerStatus, responsePayload, failureReason, false);
    }
}
