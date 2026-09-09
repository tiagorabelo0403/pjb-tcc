package com.tcc.pjb.backend.ai.common;

public class AiProviderException extends RuntimeException {

    public AiProviderException(String provedor, String detalhe) {
        super("Provedor de IA '" + provedor + "' nao concluiu a geracao: " + detalhe);
    }

    public AiProviderException(String provedor, String detalhe, Throwable causa) {
        super("Provedor de IA '" + provedor + "' nao concluiu a geracao: " + detalhe, causa);
    }
}
