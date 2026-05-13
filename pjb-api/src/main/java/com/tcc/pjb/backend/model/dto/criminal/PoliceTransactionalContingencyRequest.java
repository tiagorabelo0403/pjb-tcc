package com.tcc.pjb.backend.model.dto.criminal;

import jakarta.validation.constraints.NotBlank;

public record PoliceTransactionalContingencyRequest(
        Long inqueritoId,
        Long processoId,
        @NotBlank String familiaTransacao,
        String sistemaParceiro,
        Integer limiteTentativas,
        Boolean nativeFirstEstrito,
        String motivoOperacional
) {
    public PoliceTransactionalContingencyRequest {
        familiaTransacao = familiaTransacao == null || familiaTransacao.isBlank() ? "SNAPSHOT_PROCESSUAL" : familiaTransacao.trim().toUpperCase();
        sistemaParceiro = sistemaParceiro == null || sistemaParceiro.isBlank() ? "PJE_MNI" : sistemaParceiro.trim().toUpperCase();
        limiteTentativas = limiteTentativas == null || limiteTentativas < 1 ? 5 : Math.min(limiteTentativas, 20);
        motivoOperacional = motivoOperacional == null || motivoOperacional.isBlank() ? "CONTINGENCIA_TRANSACIONAL_POLICIAL" : motivoOperacional.trim();
    }

    public boolean nativeFirstEstritoResolvido() {
        return !Boolean.FALSE.equals(nativeFirstEstrito);
    }
}
