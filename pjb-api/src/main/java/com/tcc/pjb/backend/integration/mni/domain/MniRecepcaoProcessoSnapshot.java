package com.tcc.pjb.backend.integration.mni.domain;

public record MniRecepcaoProcessoSnapshot(Long processoId,
                                          String numeroUnificado,
                                          String connectorSystem) {
}
