package com.tcc.pjb.backend.core.guard;

public final class ProductionCriticalControlViolationException extends IllegalStateException {

    public ProductionCriticalControlViolationException(String property, String controlName) {
        super("[PRODUCTION-CONTROL-GUARD] " + controlName + " (" + property + ") está desligado em "
                + "PROD. Assinatura/guarda de chave com validade jurídica não pode subir desligada em "
                + "produção. Ligue " + property + "=true ou não use o perfil prod.");
    }
}
