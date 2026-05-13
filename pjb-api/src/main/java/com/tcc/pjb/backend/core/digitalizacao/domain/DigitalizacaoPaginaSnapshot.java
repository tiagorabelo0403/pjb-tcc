package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoPaginaSnapshot(Long paginaId,
                                          int numeroPagina,
                                          String tipoPeca,
                                          boolean revisado) {
}
