package com.tcc.pjb.backend.core.prazos.calculo.domain;

public record PrazoExecutionStatusView(
        String reference,
        String status,
        String summary
) {
}
