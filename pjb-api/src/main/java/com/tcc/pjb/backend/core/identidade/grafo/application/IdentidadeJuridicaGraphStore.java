package com.tcc.pjb.backend.core.identidade.grafo.application;

import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaGraphAggregate;
import com.tcc.pjb.backend.core.identidade.grafo.domain.IdentidadeJuridicaPersistencia;

public interface IdentidadeJuridicaGraphStore {

    IdentidadeJuridicaPersistencia persistir(IdentidadeJuridicaGraphAggregate aggregate);
}
