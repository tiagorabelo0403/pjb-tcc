package com.tcc.pjb.backend.service.intimacao;

public enum IntimacaoModalidadeEspecial {
    PESSOAL_OFICIAL_JUSTICA,
    PESSOAL_VIA_CORREIO_AR,
    PESSOAL_DEFENSOR_PUBLICO,
    PESSOAL_MINISTERIO_PUBLICO,
    PESSOAL_FAZENDA_PUBLICA,
    EDITALICIA,
    ELETRONICA_DOMICILIO_JUDICIAL,
    ELETRONICA_EMAIL_CADASTRADO,
    CARTA_PRECATORIA,
    CARTA_ROGATORIA,
    WHATSAPP_AUTORIZADO,
    DIARIO_ELETRONICO_OAB;

    public boolean exigeOficialJustica() {
        return this == PESSOAL_OFICIAL_JUSTICA;
    }

    public boolean ehEletronica() {
        return this == ELETRONICA_DOMICILIO_JUDICIAL || this == ELETRONICA_EMAIL_CADASTRADO;
    }

    public boolean ehEditalicia() {
        return this == EDITALICIA;
    }

    public boolean admiteSubstituicaoPorElectronica() {
        return switch (this) {
            case PESSOAL_DEFENSOR_PUBLICO, PESSOAL_MINISTERIO_PUBLICO, PESSOAL_FAZENDA_PUBLICA -> false;
            default -> true;
        };
    }

    public int prazoContagem() {
        return switch (this) {
            case EDITALICIA -> 20;
            case CARTA_PRECATORIA, CARTA_ROGATORIA -> 15;
            default -> 5;
        };
    }
}
