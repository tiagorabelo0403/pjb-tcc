package com.tcc.pjb.backend.core.processo.migracao.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoMigracaoIdentity(
        Long processoId,
        String numeroProcesso,
        String tribunal,
        String unidade,
        String sistemaAtual,
        String sistemaAlvo,
        List<String> marcadores
) {
    public ProcessoMigracaoIdentity(Long processoId, String numeroProcesso, String tribunal, String sistemaAtual, List<String> marcadores) {
        this(processoId, numeroProcesso, tribunal, "NAO_INFORMADA", sistemaAtual, sistemaAtual, marcadores);
    }

    public ProcessoMigracaoIdentity {
        Objects.requireNonNull(processoId);
        numeroProcesso = numeroProcesso == null ? "NAO_INFORMADO" : numeroProcesso;
        tribunal = tribunal == null ? "NAO_INFORMADO" : tribunal;
        unidade = unidade == null ? "NAO_INFORMADO" : unidade;
        sistemaAtual = sistemaAtual == null ? "NAO_DEFINIDO" : sistemaAtual;
        sistemaAlvo = sistemaAlvo == null ? "NAO_DEFINIDO" : sistemaAlvo;
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
    }
}
