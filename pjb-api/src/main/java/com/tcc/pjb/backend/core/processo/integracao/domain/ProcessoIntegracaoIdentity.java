package com.tcc.pjb.backend.core.processo.integracao.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoIntegracaoIdentity(
        Long processoId,
        String numeroProcesso,
        String tribunal,
        String unidade,
        String ramoDireito,
        String rito,
        String connectorAtual,
        List<String> marcadores
) {
    public ProcessoIntegracaoIdentity {
        Objects.requireNonNull(processoId);
        numeroProcesso = numeroProcesso == null ? "NAO_INFORMADO" : numeroProcesso;
        tribunal = tribunal == null ? "NAO_INFORMADO" : tribunal;
        unidade = unidade == null ? "NAO_INFORMADO" : unidade;
        ramoDireito = ramoDireito == null ? "NAO_INFORMADO" : ramoDireito;
        rito = rito == null ? "NAO_INFORMADO" : rito;
        connectorAtual = connectorAtual == null ? "NAO_DEFINIDO" : connectorAtual;
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
    }
}
