package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoHashQuery(
        String mniPayloadHash,
        String tribunalOrigem
) {}
