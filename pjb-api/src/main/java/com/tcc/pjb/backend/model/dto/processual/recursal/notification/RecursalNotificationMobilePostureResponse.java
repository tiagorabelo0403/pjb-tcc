package com.tcc.pjb.backend.model.dto.processual.recursal.notification;

import java.util.List;

public record RecursalNotificationMobilePostureResponse(
        String eixo,
        String titulo,
        String processoReferencia,
        String usuarioReferencia,
        String perfilDestino,
        String plataformaMobile,
        String posturaStatus,
        int posturaScore,
        boolean entregaSoberanaExterna,
        boolean canalMobileAprovado,
        String politicaAtestacao,
        String politicaBinding,
        String politicaRelay,
        List<String> politicasAplicadas,
        List<String> rotasRelacionadas,
        List<String> alertasTaticos) {
}
