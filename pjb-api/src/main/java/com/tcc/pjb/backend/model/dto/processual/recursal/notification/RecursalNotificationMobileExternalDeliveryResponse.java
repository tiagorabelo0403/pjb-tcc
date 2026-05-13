package com.tcc.pjb.backend.model.dto.processual.recursal.notification;

import java.util.List;

public record RecursalNotificationMobileExternalDeliveryResponse(
        String eixo,
        String titulo,
        String processoReferencia,
        String usuarioReferencia,
        String perfilDestino,
        String plataformaMobile,
        String dominioFederado,
        String canalEntregaExterna,
        String envelopeEntrega,
        String statusEntrega,
        boolean entregaSoberanaAprovada,
        List<String> politicasAplicadas,
        List<String> rotasRelacionadas,
        List<String> alertasTaticos) {
}
