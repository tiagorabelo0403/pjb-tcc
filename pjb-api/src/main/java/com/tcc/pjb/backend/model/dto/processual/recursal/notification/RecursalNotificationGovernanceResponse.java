package com.tcc.pjb.backend.model.dto.processual.recursal.notification;

import java.util.List;

public record RecursalNotificationGovernanceResponse(
        String eixo,
        String titulo,
        String processoReferencia,
        String usuarioReferencia,
        String perfilDestino,
        String statusFluxo,
        String canalPrioritario,
        String rotaPreviewCalendario,
        String rotaPreferenciasGlobais,
        String rotaDispatchMulticanal,
        String rotaInboxOperacional,
        boolean schedulerParaleloPermitido,
        boolean executorParaleloPermitido,
        boolean exigeCiencia,
        List<String> politicasAplicadas,
        List<String> alertasTaticos) {
}
