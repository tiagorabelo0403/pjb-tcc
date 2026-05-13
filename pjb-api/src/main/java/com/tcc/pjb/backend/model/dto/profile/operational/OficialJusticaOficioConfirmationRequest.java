package com.tcc.pjb.backend.model.dto.profile.operational;

public record OficialJusticaOficioConfirmationRequest(
        String canalConfirmacao,
        String statusEntrega,
        String protocoloEntrega,
        String referenciaExterna,
        String observacaoOperacional,
        Boolean acionarRetentativa
) {
    public boolean acionarRetentativaResolvido() {
        return Boolean.TRUE.equals(acionarRetentativa);
    }
}
