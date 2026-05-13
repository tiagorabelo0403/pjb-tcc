package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

import java.time.Instant;

public record SobrestamentoTemaWindowResult(String codigoTema, int batchSize, Instant generatedAt) {}
