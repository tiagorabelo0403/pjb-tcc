package com.tcc.pjb.backend.model.dto.criminal;

public record PoliceExecutionReconciliationRequest(
        String sistemaParceiro,
        String statusParceiro,
        String protocoloParceiro,
        String referenciaParceira,
        Boolean confirmarEntrega,
        Boolean reconciliarSnapshot,
        Boolean acionarRetentativa,
        String observacaoOperacional
) {
    public PoliceExecutionReconciliationRequest {
        sistemaParceiro = sanitizeUpper(sistemaParceiro, "PJE_MNI");
        statusParceiro = sanitizeUpper(statusParceiro, "PENDENTE");
        protocoloParceiro = sanitize(protocoloParceiro, "PROTOCOLO_PENDENTE");
        referenciaParceira = sanitize(referenciaParceira, "REFERENCIA_PENDENTE");
        observacaoOperacional = sanitize(observacaoOperacional, "RECONCILIACAO_OPERACIONAL_POLICIAL");
    }

    public boolean confirmarEntregaResolvida() {
        return !Boolean.FALSE.equals(confirmarEntrega);
    }

    public boolean reconciliarSnapshotResolvido() {
        return !Boolean.FALSE.equals(reconciliarSnapshot);
    }

    public boolean acionarRetentativaResolvido() {
        return Boolean.TRUE.equals(acionarRetentativa);
    }

    private static String sanitize(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String sanitizeUpper(String value, String fallback) {
        return sanitize(value, fallback).toUpperCase();
    }
}
