package com.tcc.pjb.backend.core.security.professional;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

public enum ProfessionalActorClass {
    ADVOCACIA,
    DEFENSORIA,
    PROCURADORIA,
    MAGISTRATURA,
    APOIO_JUDICIAL,
    OUTRO;

    public static ProfessionalActorClass of(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return usuario != null && usuario.isAdvogado() ? ADVOCACIA : OUTRO;
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo.isMagistratura()) {
            return MAGISTRATURA;
        }
        if (tipo.isDefensoriaPublica()) {
            return DEFENSORIA;
        }
        if (tipo.isProcuradoria()) {
            return PROCURADORIA;
        }
        if (tipo.isServidorJudiciario() || tipo.isAssessor()) {
            return APOIO_JUDICIAL;
        }
        if (tipo.isAdvocacia() || usuario.isAdvogado()) {
            return ADVOCACIA;
        }
        return OUTRO;
    }

    public String panelMode() {
        return switch (this) {
            case ADVOCACIA -> "ADVOCACY_PANEL";
            case DEFENSORIA -> "DEFENSORIA_PANEL";
            case PROCURADORIA -> "PROCURADORIA_PANEL";
            case MAGISTRATURA -> "MAGISTRATURA_PANEL";
            case APOIO_JUDICIAL -> "APOIO_JUDICIAL_PANEL";
            case OUTRO -> "PROFESSIONAL_PANEL";
        };
    }
}
