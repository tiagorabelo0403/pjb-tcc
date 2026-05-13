package com.tcc.pjb.backend.core.security.professional;

public enum ProfessionalActorClass {
    ADVOCACIA,
    DEFENSORIA,
    PROCURADORIA,
    MAGISTRATURA,
    APOIO_JUDICIAL,
    OUTRO;

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
