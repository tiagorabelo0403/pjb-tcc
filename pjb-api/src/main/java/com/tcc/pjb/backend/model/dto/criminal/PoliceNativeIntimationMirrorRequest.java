package com.tcc.pjb.backend.model.dto.criminal;

public record PoliceNativeIntimationMirrorRequest(
        Long inqueritoId,
        Long processoId,
        String sistemaParceiro,
        Integer janelaHoras,
        Boolean incluirEventos,
        Boolean incluirIntimacoes,
        Boolean incluirAnexos,
        Boolean reconciliarComSnapshot
) {
    public PoliceNativeIntimationMirrorRequest {
        sistemaParceiro = sistemaParceiro == null || sistemaParceiro.isBlank() ? "PJE_MNI" : sistemaParceiro.trim().toUpperCase();
        janelaHoras = janelaHoras == null || janelaHoras < 1 ? 24 : Math.min(janelaHoras, 720);
    }

    public boolean incluirEventosResolvido() {
        return !Boolean.FALSE.equals(incluirEventos);
    }

    public boolean incluirIntimacoesResolvido() {
        return !Boolean.FALSE.equals(incluirIntimacoes);
    }

    public boolean incluirAnexosResolvido() {
        return Boolean.TRUE.equals(incluirAnexos);
    }

    public boolean reconciliarComSnapshotResolvido() {
        return !Boolean.FALSE.equals(reconciliarComSnapshot);
    }
}
