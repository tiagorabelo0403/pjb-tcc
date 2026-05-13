package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoJsonPayload(String tribunalOrigem,
                                     String motivo,
                                     String xml) {
}
