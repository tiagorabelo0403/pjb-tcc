package com.tcc.pjb.backend.modules.acordo.application;

/** Entidade (processo, sala, proposta, termo, usuário) buscada por id/chave e ausente. Mapeia para 404. */
public class AcordoNotFoundException extends AcordoApplicationException {

    public AcordoNotFoundException(String message) {
        super(message);
    }
}
