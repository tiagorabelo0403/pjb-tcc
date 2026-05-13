package com.tcc.pjb.backend.integration.mni.domain;

public record MniPayloadView(
        String payloadHash,
        String motivo,
        String status
) {}
