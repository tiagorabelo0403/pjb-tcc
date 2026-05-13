package com.tcc.pjb.backend.core.criminal.custodia.domain;

import java.time.Instant;

public record AudienciaCustodiaResult(Long custodiaId,
                                      Instant prazoLimite24h) {
}
