package com.tcc.pjb.backend.core.digitalizacao.domain;

import java.math.BigDecimal;

public record DigitalizacaoConsultaResult(Long id,
                                          String status,
                                          int paginasProcessadas,
                                          Integer totalPaginas,
                                          boolean revisaoRequerida,
                                          BigDecimal confiancaMedia) {}
