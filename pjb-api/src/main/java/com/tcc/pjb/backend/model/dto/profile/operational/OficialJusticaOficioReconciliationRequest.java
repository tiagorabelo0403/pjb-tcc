package com.tcc.pjb.backend.model.dto.profile.operational;

public record OficialJusticaOficioReconciliationRequest(
        String origemReconciliacao,
        String statusParceiro,
        String referenciaParceiro,
        String hashParceiro,
        String observacao,
        Boolean repararDivergencia
) {
    public boolean repararDivergenciaResolvida() {
        return Boolean.TRUE.equals(repararDivergencia);
    }
}
