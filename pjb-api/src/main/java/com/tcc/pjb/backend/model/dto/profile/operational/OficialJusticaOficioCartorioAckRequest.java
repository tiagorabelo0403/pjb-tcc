package com.tcc.pjb.backend.model.dto.profile.operational;

public record OficialJusticaOficioCartorioAckRequest(
        String statusCartorio,
        String protocoloCartorio,
        String caixaInstitucionalCodigo,
        String observacaoCartoraria,
        Boolean juntadaMaterializada
) {
    public boolean juntadaMaterializadaResolvida() {
        return Boolean.TRUE.equals(juntadaMaterializada);
    }
}
