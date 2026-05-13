package com.tcc.pjb.backend.core.certidao;

public interface CertidaoDigitalPort {

    CertidaoDigital emitir(CertidaoRequest request);

    boolean verificar(String codigoVerificacao);
}
