package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoAuditSnapshot(Long recepcaoId,
                                       String payloadHash,
                                       String status) {
}
