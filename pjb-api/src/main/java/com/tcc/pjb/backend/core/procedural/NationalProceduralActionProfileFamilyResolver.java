package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class NationalProceduralActionProfileFamilyResolver {

    private final NationalProceduralActionProfileMessages messages;

    NationalProceduralActionProfileFamilyResolver(NationalProceduralActionProfileMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    Optional<NationalProceduralActionProfile> resolve(NationalProceduralActionProfileContext context) {
        Objects.requireNonNull(context);
        String corpus = context.corpus() == null ? "" : context.corpus();
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "INVENTARIO", "ARROLAMENTO", "PARTILHA HERANCA", "PARTILHA DE HERANCA", "SUCESSAO", "SUCESSOES")) {
            return Optional.of(profile("INVENTARIO_ARROLAMENTO", "CIVIL_SUCESSOES", true, "CIVIL_INVENTARIO_ARROLAMENTO", "SUCESSOES", messages.sucessoesReason(), messages.sucessoesLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "ALIMENTOS", "GUARDA", "VISITA", "CONVIVENCIA", "DIVORCIO", "UNIAO ESTAVEL", "PARTILHA", "INVESTIGACAO PATERNIDADE")) {
            String rito = NationalProceduralActionProfileSupport.containsAny(corpus, "ALIMENTOS")
                    ? "CIVIL_FAMILIA_ALIMENTOS"
                    : NationalProceduralActionProfileSupport.containsAny(corpus, "DIVORCIO", "UNIAO ESTAVEL")
                    ? "CIVIL_FAMILIA_DIVORCIO"
                    : "COMUM_ORDINARIO";
            return Optional.of(profile("FAMILIA", "CIVIL_FAMILIA", true, rito, "FAMILIA", messages.familiaReason(), messages.familiaLegalBase()));
        }
        return Optional.empty();
    }

    private static NationalProceduralActionProfile profile(String actionNature,
                                                           String actionFamily,
                                                           boolean specialProcedure,
                                                           String defaultRito,
                                                           String varaFamily,
                                                           String reason,
                                                           String legalBase) {
        return NationalProceduralActionProfileSupport.profile(
                actionNature,
                actionFamily,
                specialProcedure,
                defaultRito,
                varaFamily,
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                reason,
                legalBase
        );
    }
}
