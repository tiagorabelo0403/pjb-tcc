package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.core.processo.juizado.procedural.NationalProceduralJuizadoDecision;

@Component
public class NationalProceduralClassificationResolver {

    String resolveProceduralRegime(String rito,
                                   NationalProceduralActionProfile actionProfile,
                                   NationalProceduralJuizadoDecision juizadoDecision) {
        if (juizadoDecision.admiteJuizado() && juizadoDecision.ritoOverride() != null) {
            return "JUIZADO";
        }
        if (rito == null) {
            return actionProfile.specialProcedure() ? "ESPECIAL" : "COMUM";
        }
        String normalized = normalize(rito);
        if (normalized.contains("JUIZADO")) {
            return "JUIZADO";
        }
        if (normalized.contains("TRABALH")) {
            return "TRABALHISTA";
        }
        if (normalized.contains("PENAL") || normalized.contains("JURI") || normalized.contains("EXECUCAO PENAL")) {
            return "PENAL";
        }
        if (normalized.contains("ELEITORAL")) {
            return "ELEITORAL";
        }
        if (normalized.contains("MILITAR")) {
            return "MILITAR";
        }
        if (normalized.contains("PREVIDENCIARIO")) {
            return "PREVIDENCIARIO";
        }
        if (normalized.contains("INFANCIA_JUVENTUDE")) {
            return "INFANCIA_JUVENTUDE";
        }
        if (normalized.contains("ADMINISTRATIVO")) {
            return "ADMINISTRATIVO";
        }
        if (normalized.contains("EXECUCAO") || normalized.contains("CUMPRIMENTO")) {
            return "EXECUTIVO";
        }
        if (normalized.contains("ESPECIAL") || actionProfile.specialProcedure()) {
            return "ESPECIAL";
        }
        return "COMUM";
    }

    String resolveProceduralTrack(String rito,
                                  NationalProceduralActionProfile actionProfile,
                                  NationalProceduralJuizadoDecision juizadoDecision,
                                  TipoJustica tipoJustica) {
        if (juizadoDecision.ritoOverride() != null) {
            return juizadoDecision.ritoOverride();
        }
        if (rito != null) {
            return rito;
        }
        if (tipoJustica == TipoJustica.TRABALHO) {
            return "TRABALHISTA_ORDINARIO";
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return "ELEITORAL";
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
            return "MILITAR";
        }
        return firstNonBlank(actionProfile.defaultRito(), "COMUM_ORDINARIO");
    }

    private static String firstNonBlank(String... values) {
        return NationalProceduralRoutingSupport.firstNonBlank(values);
    }

    private static String normalize(String value) {
        return NationalProceduralRoutingSupport.normalize(value);
    }
}
