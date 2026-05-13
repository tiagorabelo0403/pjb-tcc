package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoRequest(
        String tribunalOrigem,
        String motivo,
        String xml
) {
}
