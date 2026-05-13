package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

import java.time.Instant;

public record SobrestamentoTemaAuditView(String codigoTema, String evento, Instant at) {}
