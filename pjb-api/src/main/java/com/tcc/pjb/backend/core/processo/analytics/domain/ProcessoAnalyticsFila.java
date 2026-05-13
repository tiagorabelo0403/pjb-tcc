package com.tcc.pjb.backend.core.processo.analytics.domain;

public record ProcessoAnalyticsFila(
        String fila,
        long volume,
        double taxaCongestionamento,
        double taxaRetrabalho,
        long urgentes,
        String severidade
) {
    public ProcessoAnalyticsFila {
        fila = fila == null || fila.isBlank() ? "SEM_FILA" : fila;
        volume = Math.max(0L, volume);
        urgentes = Math.max(0L, urgentes);
        severidade = severidade == null || severidade.isBlank() ? "INFO" : severidade;
    }
}
