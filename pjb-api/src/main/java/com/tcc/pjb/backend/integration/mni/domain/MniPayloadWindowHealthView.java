package com.tcc.pjb.backend.integration.mni.domain;

public record MniPayloadWindowHealthView(
        String reference,
        String status,
        String summary
) {
}
