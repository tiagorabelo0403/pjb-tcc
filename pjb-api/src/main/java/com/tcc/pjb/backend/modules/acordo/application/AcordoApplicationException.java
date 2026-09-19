package com.tcc.pjb.backend.modules.acordo.application;

/**
 * Base sealed pelas 3 subclasses concretas. Abstrata de propósito: cada lançamento precisa escolher
 * uma categoria HTTP (404/403/422) em vez de cair no catch-all 500 — ver
 * D-taxonomia-de-erro-http-incompleta. Ser abstrata é a trava mecânica que impede um novo call site
 * de voltar a lançar a base sem categoria.
 */
public abstract class AcordoApplicationException extends RuntimeException {

    protected AcordoApplicationException(String message) {
        super(message);
    }
}
