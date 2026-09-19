package com.tcc.pjb.backend.modules.acordo.application;

/** Ator identificado mas sem papel/vínculo/permissão para a ação (não é participante, perfil errado, não convidado). Mapeia para 403. */
public class AcordoForbiddenException extends AcordoApplicationException {

    public AcordoForbiddenException(String message) {
        super(message);
    }
}
