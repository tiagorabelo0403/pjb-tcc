package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;

public record SecretariaBalcaoVirtualMilitarRequest(
        @NotBlank String solicitanteNome,
        String protocoloAtendimento,
        String salaVirtual,
        String mensagemInicial,
        Boolean registrarEmAta
) {
    public String solicitanteNomeResolvido() {
        return normalize(solicitanteNome, "SOLICITANTE_NAO_INFORMADO");
    }

    public String protocoloAtendimentoResolvido() {
        return normalize(protocoloAtendimento, "PROTOCOLO_PENDENTE");
    }

    public String salaVirtualResolvida() {
        return normalize(salaVirtual, "SALA_VIRTUAL_MILITAR_PJB");
    }

    public String mensagemInicialResolvida() {
        return normalize(mensagemInicial, "ATENDIMENTO_VIRTUAL_INICIADO");
    }

    public boolean registrarEmAtaResolvida() {
        return registrarEmAta == null || registrarEmAta;
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
