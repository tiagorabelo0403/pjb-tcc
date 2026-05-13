package com.tcc.pjb.backend.model.dto.criminal;

public record PoliceLocalSnapshotRequest(
        Long inqueritoId,
        Long processoId,
        String escopoSnapshot,
        String sistemaParceiro,
        Boolean reconciliar,
        Boolean incluirTimeline,
        Boolean incluirAnexos,
        Boolean incluirMandados,
        Boolean congelarHash
) {
    public PoliceLocalSnapshotRequest {
        escopoSnapshot = escopoSnapshot == null || escopoSnapshot.isBlank() ? "COMPLETO" : escopoSnapshot.trim().toUpperCase();
        sistemaParceiro = sistemaParceiro == null || sistemaParceiro.isBlank() ? "PJE_MNI" : sistemaParceiro.trim().toUpperCase();
    }

    public boolean reconciliarResolvido() {
        return !Boolean.FALSE.equals(reconciliar);
    }

    public boolean incluirTimelineResolvido() {
        return !Boolean.FALSE.equals(incluirTimeline);
    }

    public boolean incluirAnexosResolvido() {
        return Boolean.TRUE.equals(incluirAnexos);
    }

    public boolean incluirMandadosResolvido() {
        return !Boolean.FALSE.equals(incluirMandados);
    }

    public boolean congelarHashResolvido() {
        return !Boolean.FALSE.equals(congelarHash);
    }
}
