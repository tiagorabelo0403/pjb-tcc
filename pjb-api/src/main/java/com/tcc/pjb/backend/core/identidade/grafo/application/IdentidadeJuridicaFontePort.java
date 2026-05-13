package com.tcc.pjb.backend.core.identidade.grafo.application;

import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaConsulta;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaSnapshot;

public interface IdentidadeJuridicaFontePort {

    String codigoFonte();

    default int prioridade() {
        return 100;
    }

    default boolean suporta(IdentidadeJuridicaConsulta consulta) {
        return true;
    }

    IdentidadeJuridicaSnapshot resolver(IdentidadeJuridicaConsulta consulta);
}
