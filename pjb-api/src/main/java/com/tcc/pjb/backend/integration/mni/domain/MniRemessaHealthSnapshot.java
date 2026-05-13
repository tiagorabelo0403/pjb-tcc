package com.tcc.pjb.backend.integration.mni.domain;

public record MniRemessaHealthSnapshot(Long remessaId,
                                       String status,
                                       int tentativas,
                                       boolean failurePresent) {}
