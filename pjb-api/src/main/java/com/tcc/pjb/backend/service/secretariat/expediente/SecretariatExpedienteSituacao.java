package com.tcc.pjb.backend.service.secretariat.expediente;

public enum SecretariatExpedienteSituacao {
    PENDENTE,
    SEM_REGISTRO_INTIMACAO,
    CONFIRMADA_DENTRO_PRAZO,
    PRAZO_ENCERRADO,
    SEM_PRAZO;

    public boolean requerAtencao() {
        return this == PENDENTE || this == SEM_REGISTRO_INTIMACAO || this == PRAZO_ENCERRADO;
    }

    public String rotulo() {
        return switch (this) {
            case PENDENTE -> "Expediente pendente de ciência";
            case SEM_REGISTRO_INTIMACAO -> "Sem registro de intimação";
            case CONFIRMADA_DENTRO_PRAZO -> "Ciência confirmada dentro do prazo";
            case PRAZO_ENCERRADO -> "Prazo encerrado sem ciência";
            case SEM_PRAZO -> "Expediente sem prazo definido";
        };
    }
}
