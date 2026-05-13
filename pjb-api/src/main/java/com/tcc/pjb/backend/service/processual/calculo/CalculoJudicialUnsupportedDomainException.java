package com.tcc.pjb.backend.service.processual.calculo;

import java.util.List;

public final class CalculoJudicialUnsupportedDomainException extends IllegalArgumentException {

    private final String requestedDomain;
    private final String normalizedDomain;
    private final List<String> supportedDomains;
    private final String suggestedDomain;

    public CalculoJudicialUnsupportedDomainException(String requestedDomain,
                                                     String normalizedDomain,
                                                     List<String> supportedDomains,
                                                     String suggestedDomain) {
        super("Dominio de calculo judicial nao suportado.");
        this.requestedDomain = requestedDomain;
        this.normalizedDomain = normalizedDomain;
        this.supportedDomains = List.copyOf(supportedDomains);
        this.suggestedDomain = suggestedDomain;
    }

    public String getRequestedDomain() {
        return requestedDomain;
    }

    public String getNormalizedDomain() {
        return normalizedDomain;
    }

    public List<String> getSupportedDomains() {
        return supportedDomains;
    }

    public String getSuggestedDomain() {
        return suggestedDomain;
    }
}
