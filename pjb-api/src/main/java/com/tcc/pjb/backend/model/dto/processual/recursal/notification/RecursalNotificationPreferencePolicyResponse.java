package com.tcc.pjb.backend.model.dto.processual.recursal.notification;

import java.util.List;

public record RecursalNotificationPreferencePolicyResponse(
        String eixo,
        String titulo,
        String processoReferencia,
        String usuarioReferencia,
        String perfilDestino,
        String canalPreferencial,
        List<String> canaisHabilitados,
        String politicaPerfil,
        String politicaFederada,
        List<String> preferenciasAplicadas,
        List<String> politicasAplicadas,
        List<String> rotasRelacionadas,
        List<String> alertasTaticos) {
}
