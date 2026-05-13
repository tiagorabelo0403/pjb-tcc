package com.tcc.pjb.backend.integration.mni.domain;
public record MniRemessaEnvelopeAuditSnapshot(Long processoId, String tribunalDestino, String motivo, boolean confirmed) {}
