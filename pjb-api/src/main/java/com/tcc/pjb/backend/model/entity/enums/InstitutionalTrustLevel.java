package com.tcc.pjb.backend.model.entity.enums;

public enum InstitutionalTrustLevel {
    NIVEL_1_IDENTIDADE_FEDERADA(1),
    NIVEL_2_NOMEACAO_ATIVA(2),
    NIVEL_3_CERTIFICADO_QUALIFICADO(3),
    NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO(4);

    public static final InstitutionalTrustLevel NIVEL_2_MFA_FORTE = NIVEL_2_NOMEACAO_ATIVA;
    public static final InstitutionalTrustLevel ALTO = NIVEL_3_CERTIFICADO_QUALIFICADO;
    public static final InstitutionalTrustLevel ELEVADO = NIVEL_3_CERTIFICADO_QUALIFICADO;

    private final int ordem;

    InstitutionalTrustLevel(int ordem) {
        this.ordem = ordem;
    }

    public int ordem() {
        return ordem;
    }

    public boolean atende(InstitutionalTrustLevel minimo) {
        return minimo == null || this.ordem >= minimo.ordem;
    }
}
