package com.tcc.pjb.backend.ai.juridica.symbolic;

import java.util.List;

public final class LegalSymbolicValidationCatalog {

    public static final String ENGINE_PRAZO = "PRAZO";
    public static final String ENGINE_COMPETENCIA = "COMPETENCIA";
    public static final String ENGINE_CABIMENTO = "CABIMENTO";
    public static final String ENGINE_SIGILO = "SIGILO";
    public static final String ENGINE_PROCEDURAL_COMPATIBILITY = "PROCEDURAL_COMPATIBILITY";

    private LegalSymbolicValidationCatalog() {
    }

    public static List<String> standardV3Engines() {
        return List.of(
                ENGINE_PRAZO,
                ENGINE_COMPETENCIA,
                ENGINE_CABIMENTO,
                ENGINE_SIGILO,
                ENGINE_PROCEDURAL_COMPATIBILITY
        );
    }
}
