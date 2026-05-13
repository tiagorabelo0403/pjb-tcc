package com.tcc.pjb.backend.service.processual.participacao;

import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public enum Persona {
    ADVOCACIA_PRIVADA("Advocacia privada"),
    DEFENSORIA("Defensoria Pública"),
    PROCURADORIA("Procuradoria / advocacia pública"),
    MINISTERIO_PUBLICO("Ministério Público"),
    PERICIA("Perícia e apoio técnico"),
    NAO_SUPORTADA("Perfil não suportado");

    private final String label;

    Persona(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Persona resolve(TipoUsuario tipoUsuario) {
        if (tipoUsuario == null) {
            return NAO_SUPORTADA;
        }
        if (tipoUsuario.isAdvocacia()) {
            return ADVOCACIA_PRIVADA;
        }
        if (tipoUsuario.isDefensoriaPublica()) {
            return DEFENSORIA;
        }
        if (tipoUsuario.isProcuradoria()) {
            return PROCURADORIA;
        }
        if (tipoUsuario.isMinisterioPublico()) {
            return MINISTERIO_PUBLICO;
        }
        if (tipoUsuario.isPerito()) {
            return PERICIA;
        }
        return NAO_SUPORTADA;
    }
}
