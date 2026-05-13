package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class NationalProceduralActionProfilePropertyResolver {

    private final NationalProceduralActionProfileMessages messages;

    NationalProceduralActionProfilePropertyResolver(NationalProceduralActionProfileMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    Optional<NationalProceduralActionProfile> resolve(NationalProceduralActionProfileContext context) {
        Objects.requireNonNull(context);
        String corpus = context.corpus() == null ? "" : context.corpus();
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "USUCAPIAO")) {
            return Optional.of(profile("USUCAPIAO", "CIVIL_IMOBILIARIO", true, "CIVIL_USUCAPIAO", "IMOBILIARIO"));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "POSSESSORIA", "REINTEGRACAO POSSE", "MANUTENCAO POSSE", "INTERDITO PROIBITORIO")) {
            String rito = NationalProceduralActionProfileSupport.containsAny(corpus, "INTERDITO PROIBITORIO")
                    ? "CIVIL_INTERDITO_PROIBITORIO"
                    : "CIVIL_POSSESSORIA";
            return Optional.of(profile("POSSESSORIA", "CIVIL_IMOBILIARIO", true, rito, "IMOBILIARIO"));
        }
        return Optional.empty();
    }

    private NationalProceduralActionProfile profile(String actionNature,
                                                    String actionFamily,
                                                    boolean specialProcedure,
                                                    String defaultRito,
                                                    String varaFamily) {
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
                messages.civelReason(),
                messages.civelLegalBase()
        );
    }
}
