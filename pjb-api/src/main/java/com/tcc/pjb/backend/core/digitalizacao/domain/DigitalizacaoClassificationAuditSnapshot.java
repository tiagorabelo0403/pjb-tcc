package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.math.BigDecimal;

public record DigitalizacaoClassificationAuditSnapshot(Long paginaId, String tipoPeca, BigDecimal confianca) {}
