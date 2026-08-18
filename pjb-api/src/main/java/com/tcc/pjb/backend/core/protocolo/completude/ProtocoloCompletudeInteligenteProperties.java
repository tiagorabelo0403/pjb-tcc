package com.tcc.pjb.backend.core.protocolo.completude;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.protocolo.completude.ia")
public class ProtocoloCompletudeInteligenteProperties {

    private double limiarBloqueio = 0.85;

    public double getLimiarBloqueio() {
        return limiarBloqueio;
    }

    public void setLimiarBloqueio(double limiarBloqueio) {
        this.limiarBloqueio = limiarBloqueio;
    }

    public double limiarBloqueio() {
        return limiarBloqueio;
    }
}
