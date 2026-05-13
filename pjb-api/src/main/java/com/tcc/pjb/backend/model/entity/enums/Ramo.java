package com.tcc.pjb.backend.model.entity.enums;


public enum Ramo {

    CIVIL,
    FAMILIA,
    CONSUMIDOR,
    EMPRESARIAL,
    PENAL,
    MILITAR,
    ELEITORAL,
    ADMINISTRATIVO,
    TRIBUTARIO,
    CONSTITUCIONAL,
    AMBIENTAL,
    TRABALHISTA,
    PREVIDENCIARIO,
    INFANCIA_JUVENTUDE,
    AGRARIO,
    OUTROS;

    public RamoDireito toRamoDireito() {
        return switch (this) {
            case CIVIL -> RamoDireito.CIVIL;
            case FAMILIA -> RamoDireito.FAMILIA;
            case CONSUMIDOR -> RamoDireito.CONSUMIDOR;
            case EMPRESARIAL -> RamoDireito.EMPRESARIAL;
            case PENAL -> RamoDireito.PENAL;
            case MILITAR -> RamoDireito.MILITAR;
            case ELEITORAL -> RamoDireito.ELEITORAL;
            case ADMINISTRATIVO -> RamoDireito.ADMINISTRATIVO;
            case TRIBUTARIO -> RamoDireito.TRIBUTARIO;
            case CONSTITUCIONAL -> RamoDireito.CONSTITUCIONAL;
            case AMBIENTAL -> RamoDireito.AMBIENTAL;
            case TRABALHISTA -> RamoDireito.TRABALHISTA;
            case PREVIDENCIARIO -> RamoDireito.PREVIDENCIARIO;
            case INFANCIA_JUVENTUDE -> RamoDireito.INFANCIA_JUVENTUDE;
            case AGRARIO -> RamoDireito.AGRARIO;
            case OUTROS -> RamoDireito.CIVIL; 
            default -> RamoDireito.CIVIL;
        };
    }

    public static Ramo from(RamoDireito ramoDireito) {
        if (ramoDireito == null) return OUTROS;
        try {
            return Ramo.valueOf(ramoDireito.name());
        } catch (IllegalArgumentException e) {
            return OUTROS;
        }
    }
}
