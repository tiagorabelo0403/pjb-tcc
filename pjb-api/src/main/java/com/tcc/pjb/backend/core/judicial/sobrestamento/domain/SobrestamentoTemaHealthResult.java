package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

import java.time.Instant;

public record SobrestamentoTemaHealthResult(String codigoTema, boolean temaEncontrado, long pendentes, Instant checkedAt) {}
