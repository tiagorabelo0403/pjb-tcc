package com.tcc.pjb.backend.ai.juridica.v3.core;

import java.util.List;

public record RamoDescriptor(
        String codigo,
        String nome,
        String regraPrincipal,
        String codigoProcessual,
        List<String> subRamos,
        List<String> tribunaisCompetentes,
        List<String> classesComuns,
        List<String> precedentesEstruturantes,
        List<String> prazosPrincipais,
        boolean segredoJustica,
        boolean exigeMP,
        boolean admiteConciliacao
) {
}
