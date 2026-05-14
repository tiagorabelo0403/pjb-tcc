package com.tcc.pjb.backend.service.citacao;

public enum ModalidadeCitacao {
    MANDADO,
    POSTAL,
    EDITAL,
    ELETRONICA_DOMICILIO,
    CARTA_PRECATORIA,
    CARTA_ROGATORIA,
    HORA_CERTA,
    PESSOAL_FAZENDA;

    public boolean eEletronica() {
        return this == ELETRONICA_DOMICILIO;
    }

    public boolean requerOfficialDeJustica() {
        return this == MANDADO || this == HORA_CERTA;
    }

    public boolean geraArRetorno() {
        return this == POSTAL || this == CARTA_PRECATORIA;
    }
}
