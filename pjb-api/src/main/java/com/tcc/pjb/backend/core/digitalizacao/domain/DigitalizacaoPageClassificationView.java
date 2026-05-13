package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoPageClassificationView(Long paginaId,
                                                  Integer numeroPagina,
                                                  String tipoPeca,
                                                  Double confianca) {}
