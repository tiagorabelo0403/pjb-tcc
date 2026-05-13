package com.tcc.pjb.backend.core.dje.domain;

public record DjeFalhaEnvioResult(Long djeId,
                                  String reason,
                                  boolean failed) {
}
