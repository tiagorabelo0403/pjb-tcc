package com.tcc.pjb.backend.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RecursoNaoEncontradoException extends RuntimeException {
    private final String recurso;
    private final Object identificador;

    public RecursoNaoEncontradoException(String message) {
        super(message);
        this.recurso = "Desconhecido";
        this.identificador = null;
    }
    public RecursoNaoEncontradoException(String recurso, Object identificador) {
        super(String.format("Recurso '%s' não encontrado com identificador: %s", recurso, identificador));
        this.recurso = recurso;
        this.identificador = identificador;
    }
    public String getRecurso() {
        return recurso;
    }
    public Object getIdentificador() {
        return identificador;
    }
}
