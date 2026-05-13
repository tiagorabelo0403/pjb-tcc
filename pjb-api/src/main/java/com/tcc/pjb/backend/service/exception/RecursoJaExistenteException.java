package com.tcc.pjb.backend.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RecursoJaExistenteException extends RuntimeException {
    public RecursoJaExistenteException(String message) {
        super(message);
    }
}