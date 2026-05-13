package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoCommand(String tribunalOrigem, String motivo, String xml) {
    public MniRecepcaoRequest toRequest() {
        return new MniRecepcaoRequest(tribunalOrigem, motivo, xml);
    }
}
