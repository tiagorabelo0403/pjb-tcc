package com.tcc.pjb.backend.model.dto.profile.operational;

public record OficialJusticaOficioChannelAckRequest(
        String canalConfirmado,
        String statusCanal,
        String providerReference,
        String protocoloCanal,
        String observacaoOperacional,
        Boolean entregaDefinitiva
) {
    public boolean entregaDefinitivaResolvida() {
        return Boolean.TRUE.equals(entregaDefinitiva);
    }
}
