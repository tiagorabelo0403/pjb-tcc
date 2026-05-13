package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoHashResult(
        String mniPayloadHash,
        String tribunalOrigem,
        Long processoIdLocal,
        String status,
        boolean duplicated
) {}
