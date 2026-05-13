package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.math.BigDecimal;

public record OcrPageSnapshot(Long paginaId,
                              Integer numeroPagina,
                              String tipoPeca,
                              BigDecimal confianca,
                              boolean revisado) {}
