package com.tcc.pjb.backend.model.dto.processual.recursal.notification;

public record RecursalNotificationMobileHardeningRequest(
        RecursalNotificationPreferencePolicyRequest preferencias,
        boolean dispositivoConfiavel,
        boolean appSoberanoAtestado,
        boolean relaySoberanoAtivo,
        boolean tokenVinculadoAoDispositivo,
        boolean antiReplayAtivo,
        boolean criptografiaPontaAPontaAtiva,
        boolean biometriaLocalAtiva,
        String plataformaMobile) {
}
