package com.tcc.pjb.backend.core.digitalizacao.domain;
import java.math.BigDecimal;
public record DigitalizacaoJobAuditSnapshot(Long jobId, String status, Integer paginasProcessadas, BigDecimal confiancaMedia) {}
