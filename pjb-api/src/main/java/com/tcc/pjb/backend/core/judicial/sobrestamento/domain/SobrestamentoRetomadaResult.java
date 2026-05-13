package com.tcc.pjb.backend.core.judicial.sobrestamento.domain;

import java.time.Instant;

public record SobrestamentoRetomadaResult(String codigoTema, String resultado, int totalRetomado, Instant executadoEm) {}
