package com.tcc.pjb.backend.model.dto.processual.recursal.notification;

import java.util.List;

public record RecursalNotificationScienceResponse(
        String eixo,
        String titulo,
        String processoReferencia,
        String usuarioReferencia,
        String tokenRastreio,
        String statusCiencia,
        String rotaTrackingGif,
        String rotaTrackingCiencia,
        boolean cienciaRegistrada,
        boolean reaberturaPrazoPermitida,
        List<String> politicasAplicadas,
        List<String> alertasTaticos) {
}
