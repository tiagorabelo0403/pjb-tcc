package com.tcc.pjb.backend.modules.atendimento.service;

public class AtendimentoTosNotAcceptedException extends RuntimeException {

    private final int requiredVersion;
    private final String tosUrl;

    public AtendimentoTosNotAcceptedException(int requiredVersion, String tosUrl) {
        super("tos_not_accepted");
        this.requiredVersion = requiredVersion;
        this.tosUrl = tosUrl;
    }

    public int requiredVersion() {
        return requiredVersion;
    }

    public String tosUrl() {
        return tosUrl;
    }
}
