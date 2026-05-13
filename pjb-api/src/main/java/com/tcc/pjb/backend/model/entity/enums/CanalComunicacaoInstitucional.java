package com.tcc.pjb.backend.model.entity.enums;

public enum CanalComunicacaoInstitucional {
    PJB_INBOX,
    DOMICILIO_JUDICIAL_ELETRONICO,
    DJEN,
    WEBHOOK_INSTITUCIONAL,
    PORTAL_LEGADO_INTEGRADO,
    COMUNICACAO_FISICA_OFICIAL,
    EMAIL_AVISO,
    SMS_AVISO,
    PUSH_AVISO;

    public boolean isPrincipalJuridico() {
        return switch (this) {
            case PJB_INBOX,
                    DOMICILIO_JUDICIAL_ELETRONICO,
                    DJEN,
                    WEBHOOK_INSTITUCIONAL,
                    PORTAL_LEGADO_INTEGRADO,
                    COMUNICACAO_FISICA_OFICIAL -> true;
            default -> false;
        };
    }

    public boolean isAvisoInformativo() {
        return !isPrincipalJuridico();
    }
}
