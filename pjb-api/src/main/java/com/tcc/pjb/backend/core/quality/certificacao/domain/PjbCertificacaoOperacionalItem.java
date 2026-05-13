package com.tcc.pjb.backend.core.quality.certificacao.domain;

import java.util.Objects;

public record PjbCertificacaoOperacionalItem(String codigo,
                                             String categoria,
                                             String severidade,
                                             boolean conforme,
                                             String diagnostico,
                                             String acaoCorretiva) {
    public PjbCertificacaoOperacionalItem {
        codigo = Objects.toString(codigo, "").trim();
        categoria = Objects.toString(categoria, "").trim();
        severidade = Objects.toString(severidade, "").trim();
        diagnostico = Objects.toString(diagnostico, "").trim();
        acaoCorretiva = Objects.toString(acaoCorretiva, "").trim();
    }
}
