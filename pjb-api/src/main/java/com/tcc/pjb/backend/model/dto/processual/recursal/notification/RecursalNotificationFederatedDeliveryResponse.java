package com.tcc.pjb.backend.model.dto.processual.recursal.notification;

import java.util.List;

public record RecursalNotificationFederatedDeliveryResponse(
        String eixo,
        String titulo,
        String processoReferencia,
        String usuarioReferencia,
        String perfilDestino,
        String dominioFederado,
        boolean entregaExternaSoberana,
        String canalExternoPrioritario,
        String politicaFederada,
        String assinaturaEntrega,
        List<String> politicasAplicadas,
        List<String> rotasRelacionadas,
        List<String> alertasTaticos) {
}
