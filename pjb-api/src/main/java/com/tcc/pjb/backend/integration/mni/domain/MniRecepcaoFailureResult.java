package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoFailureResult(Long recepcaoId,
                                       String status,
                                       String failureReason) {}
