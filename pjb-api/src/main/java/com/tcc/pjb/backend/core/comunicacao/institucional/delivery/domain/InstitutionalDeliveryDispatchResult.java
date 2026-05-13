package com.tcc.pjb.backend.core.comunicacao.institucional.delivery.domain;

import com.tcc.pjb.backend.model.entity.enums.MotivoFalhaEntregaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.StatusTentativaEntregaInstitucional;

public record InstitutionalDeliveryDispatchResult(
        StatusTentativaEntregaInstitucional status,
        boolean transientFailure,
        MotivoFalhaEntregaInstitucional failureReason,
        String providerReference,
        String providerStatus,
        String detail
) {
    public InstitutionalDeliveryDispatchResult {
        if (status == null) {
            throw new IllegalArgumentException("status é obrigatório");
        }
    }

    public static InstitutionalDeliveryDispatchResult entregue(String providerReference, String providerStatus, String detail) {
        return new InstitutionalDeliveryDispatchResult(StatusTentativaEntregaInstitucional.ENTREGUE, false, null, providerReference, providerStatus, detail);
    }

    public static InstitutionalDeliveryDispatchResult encaminhada(String providerReference, String providerStatus, String detail) {
        return new InstitutionalDeliveryDispatchResult(StatusTentativaEntregaInstitucional.ENCAMINHADA, false, null, providerReference, providerStatus, detail);
    }

    public static InstitutionalDeliveryDispatchResult retry(MotivoFalhaEntregaInstitucional failureReason, String providerStatus, String detail) {
        return new InstitutionalDeliveryDispatchResult(StatusTentativaEntregaInstitucional.RETRY_AGENDADO, true, failureReason, null, providerStatus, detail);
    }

    public static InstitutionalDeliveryDispatchResult falhaTerminal(MotivoFalhaEntregaInstitucional failureReason, String providerStatus, String detail) {
        return new InstitutionalDeliveryDispatchResult(StatusTentativaEntregaInstitucional.FALHA_TERMINAL, false, failureReason, null, providerStatus, detail);
    }
}
