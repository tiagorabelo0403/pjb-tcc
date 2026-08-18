package com.tcc.pjb.backend.core.security.scope;

import java.util.Optional;

public interface PjbObjectScopeGuard {

    void requireAccess(TipoObjetoProtegido tipo, Long objetoId, AcaoEscopo acao);

    boolean canAccess(TipoObjetoProtegido tipo, Long objetoId, AcaoEscopo acao);

    Optional<EscopoNegado> explicar(TipoObjetoProtegido tipo, Long objetoId, AcaoEscopo acao);
}
