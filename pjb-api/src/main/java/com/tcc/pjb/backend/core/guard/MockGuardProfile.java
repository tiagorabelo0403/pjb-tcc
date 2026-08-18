package com.tcc.pjb.backend.core.guard;

public enum MockGuardProfile {
    PROD, STAGING, HOMOLOG, DEV, TEST, LOCAL;

    public boolean isRealEnvironment() {
        return this == PROD || this == STAGING || this == HOMOLOG;
    }

    public static MockGuardProfile fromProfileName(String name) {
        if (name == null) {
            return DEV;
        }
        return switch (name.toLowerCase()) {
            case "prod", "production" -> PROD;
            case "staging" -> STAGING;
            case "homolog", "homologacao", "homologação" -> HOMOLOG;
            case "test", "testes" -> TEST;
            case "local" -> LOCAL;
            default -> DEV;
        };
    }
}
