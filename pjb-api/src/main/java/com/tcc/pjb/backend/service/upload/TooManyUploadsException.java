package com.tcc.pjb.backend.service.upload;

public class TooManyUploadsException extends RuntimeException {
    public TooManyUploadsException(String message) {
        super(message);
    }
}
