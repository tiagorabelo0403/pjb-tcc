package com.tcc.pjb.backend.model.dto.processual.recursal.notification;

import com.tcc.pjb.backend.model.dto.processual.recursal.automation.RecursalAutomationRequest;

public record RecursalNotificationGovernanceRequest(
        RecursalAutomationRequest contexto,
        String processoReferencia,
        String usuarioReferencia,
        String perfilDestino,
        boolean sigiloso,
        boolean urgente,
        boolean mobileAtivo,
        boolean exigeCiencia,
        int prazoCriticoHoras,
        String tokenRastreio) {
}
