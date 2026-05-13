package com.tcc.pjb.backend.core.processo.recursal.domain.automation;

import java.util.Objects;

public record RecursalAutomationSignal(
        String codigo,
        RecursalAutomationSignalStatus status,
        String mensagem) {

    public RecursalAutomationSignal {
        codigo = Objects.requireNonNull(codigo, "codigo").trim();
        status = Objects.requireNonNull(status, "status");
        mensagem = Objects.requireNonNull(mensagem, "mensagem").trim();
        if (codigo.isBlank()) {
            throw new IllegalArgumentException("codigo é obrigatório");
        }
        if (mensagem.isBlank()) {
            throw new IllegalArgumentException("mensagem é obrigatória");
        }
    }
}
