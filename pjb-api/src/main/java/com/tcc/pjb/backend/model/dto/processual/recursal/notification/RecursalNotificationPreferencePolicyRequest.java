package com.tcc.pjb.backend.model.dto.processual.recursal.notification;

public record RecursalNotificationPreferencePolicyRequest(
        RecursalNotificationGovernanceRequest governanca,
        boolean canalPushHabilitado,
        boolean canalInboxHabilitado,
        boolean canalCalendarioHabilitado,
        boolean canalEmailHabilitado,
        boolean canalSmsHabilitado,
        boolean federacaoInstitucionalAtiva,
        boolean dominioSoberanoExterno,
        String dominioFederadoReferencia) {
}
