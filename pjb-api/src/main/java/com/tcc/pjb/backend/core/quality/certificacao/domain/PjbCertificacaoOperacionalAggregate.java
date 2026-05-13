package com.tcc.pjb.backend.core.quality.certificacao.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record PjbCertificacaoOperacionalAggregate(Long processoId,
                                                  String numeroProcesso,
                                                  List<PjbCertificacaoOperacionalItem> itens,
                                                  int percentualCobertura,
                                                  boolean possuiFalhaCritica,
                                                  List<String> modulosCriticos,
                                                  Instant geradoEm) {
    public PjbCertificacaoOperacionalAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        itens = itens == null ? List.of() : List.copyOf(itens);
        modulosCriticos = modulosCriticos == null ? List.of() : List.copyOf(modulosCriticos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }

    public boolean existeFalhaCritica() {
        return possuiFalhaCritica();
    }

    public int cobertura() {
        return percentualCobertura();
    }

    public boolean critico() {
        return possuiFalhaCritica();
    }
}
