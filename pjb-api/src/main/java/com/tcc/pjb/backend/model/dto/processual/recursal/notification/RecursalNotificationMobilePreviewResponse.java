package com.tcc.pjb.backend.model.dto.processual.recursal.notification;

import java.util.List;

public record RecursalNotificationMobilePreviewResponse(
        String eixo,
        String titulo,
        String processoReferencia,
        String usuarioReferencia,
        String perfilDestino,
        String canalPrioritario,
        String janelaCriticidade,
        String audienciaAlvo,
        boolean mobileAtivo,
        boolean sigiloReforcado,
        List<String> alertasPendentes,
        List<String> politicasAplicadas,
        List<String> rotasRelacionadas) {
}
