package com.tcc.pjb.backend.integration.mni.domain;

public record MniRemessaAuditSnapshot(Long remessaId,
                                      String payloadHash,
                                      String failureReason) {
}
