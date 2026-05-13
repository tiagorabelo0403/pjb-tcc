package com.tcc.pjb.backend.service.processual.participacao.submission;

import java.time.Instant;

public record SubmissionAuditView(Long actorUserId,
                                  String actorRole,
                                  String certificadoSerialMascarado,
                                  String assinaturaModo,
                                  Instant protocoladoEm,
                                  String ack) {
}
