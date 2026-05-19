package com.tcc.pjb.backend.modules.acordo.domain;

public enum AcordoPapelParticipante {
    PARTE,
    ADVOGADO,
    CONCILIADOR,
    MEDIADOR,
    MAGISTRADO,
    SERVIDOR_AUTORIZADO;

    public boolean gestorDaSala() {
        return this == CONCILIADOR
                || this == MEDIADOR
                || this == MAGISTRADO
                || this == SERVIDOR_AUTORIZADO;
    }

    public boolean podeAssinarTermo() {
        return this == PARTE || this == ADVOGADO || this == MAGISTRADO;
    }
}
