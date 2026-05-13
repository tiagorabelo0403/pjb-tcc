package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

import java.time.Instant;

public record SobrestamentoTemaSnapshot(Long processoId, Long temaId, String statusAnterior, Instant sobrestadoEm, Instant retomadoEm) {}
