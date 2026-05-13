package com.tcc.pjb.backend.core.digitalizacao.domain;
import java.math.BigDecimal;
public record DigitalizacaoPageAuditSnapshot(Long paginaId, Integer numeroPagina, String tipoPeca, BigDecimal confianca, Boolean revisado) {}
