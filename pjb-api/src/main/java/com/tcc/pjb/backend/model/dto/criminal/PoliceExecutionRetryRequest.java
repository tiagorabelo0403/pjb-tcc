package com.tcc.pjb.backend.model.dto.criminal;

public record PoliceExecutionRetryRequest(
        String sistemaParceiro,
        String motivoOperacional,
        Integer tentativaForcada,
        Boolean manterNativeFirstEstrito
) {
    public PoliceExecutionRetryRequest {
        sistemaParceiro = sistemaParceiro == null || sistemaParceiro.isBlank() ? "PJE_MNI" : sistemaParceiro.trim().toUpperCase();
        motivoOperacional = motivoOperacional == null || motivoOperacional.isBlank() ? "RETENTATIVA_OPERACIONAL_POLICIAL" : motivoOperacional.trim();
        tentativaForcada = tentativaForcada == null || tentativaForcada < 1 ? 0 : Math.min(tentativaForcada, 20);
    }

    public int tentativaForcadaResolvida() {
        return tentativaForcada == null ? 0 : tentativaForcada;
    }

    public boolean manterNativeFirstEstritoResolvido() {
        return !Boolean.FALSE.equals(manterNativeFirstEstrito);
    }
}
