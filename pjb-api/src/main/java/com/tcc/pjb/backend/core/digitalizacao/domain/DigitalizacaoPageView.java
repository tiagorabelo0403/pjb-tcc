package com.tcc.pjb.backend.core.digitalizacao.domain;

public record DigitalizacaoPageView(Long paginaId,
                                    int numeroPagina,
                                    String tipoPeca,
                                    boolean revisado) {
}
