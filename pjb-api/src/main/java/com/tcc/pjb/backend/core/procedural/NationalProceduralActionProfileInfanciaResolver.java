package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class NationalProceduralActionProfileInfanciaResolver {

    private final NationalProceduralActionProfileMessages messages;

    NationalProceduralActionProfileInfanciaResolver(NationalProceduralActionProfileMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    Optional<NationalProceduralActionProfile> resolve(NationalProceduralActionProfileContext context) {
        Objects.requireNonNull(context);
        String corpus = context.corpus() == null ? "" : context.corpus();
        if (!isInfanciaContext(context, corpus)) {
            return Optional.empty();
        }
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (NationalProceduralActionProfileSupport.containsAny(corpus,
                "ATO INFRACIONAL",
                "MEDIDA SOCIOEDUCATIVA",
                "APURACAO DE ATO INFRACIONAL",
                "APURAÇÃO DE ATO INFRACIONAL",
                "REMISSAO",
                "REMISSÃO",
                "SEMILIBERDADE",
                "INTERNACAO",
                "INTERNAÇÃO",
                "ADOLESCENTE EM CONFLITO COM A LEI")) {
            alerts.add(messages.infanciaInfracionalAlert());
            reviewChecklist.add(messages.infanciaInfracionalChecklist());
            return Optional.of(rebuilt(profile("ATO_INFRACIONAL_ECA", true, "INFANCIA_JUVENTUDE_INFRACIONAL", messages.infanciaInfracionalReason(), messages.infanciaInfracionalLegalBase()), alerts, reviewChecklist));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus,
                "ADOCAO",
                "ADOÇÃO",
                "HABILITACAO A ADOCAO",
                "HABILITAÇÃO À ADOÇÃO",
                "ESTAGIO DE CONVIVENCIA",
                "ESTÁGIO DE CONVIVÊNCIA",
                "PRETENSAO ADOTIVA",
                "PRETENSÃO ADOTIVA")) {
            reviewChecklist.add(messages.infanciaAdocaoChecklist());
            return Optional.of(rebuilt(profile("ADOCAO_ECA", true, "INFANCIA_JUVENTUDE_ADOCAO", messages.infanciaAdocaoReason(), messages.infanciaAdocaoLegalBase()), alerts, reviewChecklist));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus,
                "GUARDA",
                "TUTELA",
                "CURATELA",
                "RESPONSAVEL LEGAL",
                "RESPONSÁVEL LEGAL",
                "MENOR",
                "CRIANCA",
                "CRIANÇA",
                "ADOLESCENTE")
                && NationalProceduralActionProfileSupport.containsAny(corpus, "MENOR", "CRIANCA", "CRIANÇA", "ADOLESCENTE", "ECA", "INFANCIA", "INFÂNCIA")) {
            reviewChecklist.add(messages.infanciaTutelaMenorChecklist());
            return Optional.of(rebuilt(profile("TUTELA_CURATELA_MENOR", true, "INFANCIA_JUVENTUDE_TUTELA_CURATELA_MENOR", messages.infanciaTutelaMenorReason(), messages.infanciaTutelaMenorLegalBase()), alerts, reviewChecklist));
        }
        alerts.add(messages.infanciaProtecaoAlert());
        reviewChecklist.add(messages.infanciaProtecaoChecklist());
        return Optional.of(rebuilt(profile("PROTECAO_INTEGRAL_ECA", true, "INFANCIA_JUVENTUDE_ECA", messages.infanciaProtecaoReason(), messages.infanciaProtecaoLegalBase()), alerts, reviewChecklist));
    }

    private static NationalProceduralActionProfile profile(String actionNature,
                                                           boolean specialProcedure,
                                                           String defaultRito,
                                                           String reason,
                                                           String legalBase) {
        return NationalProceduralActionProfileSupport.profile(
                actionNature,
                "INFANCIA_JUVENTUDE",
                specialProcedure,
                defaultRito,
                "INFANCIA_JUVENTUDE",
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                reason,
                legalBase
        );
    }

    private static NationalProceduralActionProfile rebuilt(NationalProceduralActionProfile profile,
                                                           LinkedHashSet<String> alerts,
                                                           LinkedHashSet<String> reviewChecklist) {
        return NationalProceduralActionProfileSupport.rebuilt(profile, alerts, reviewChecklist);
    }

    private static boolean isInfanciaContext(NationalProceduralActionProfileContext context,
                                             String corpus) {
        ProceduralCanonicalResolver.CanonicalContext canonical = context.canonical();
        boolean canonicalInfancia = canonical != null && ((canonical.rito() != null && canonical.rito().isInfancia())
                || "INFANCIA_JUVENTUDE".equalsIgnoreCase(canonical.ramoDireito()));
        return canonicalInfancia || NationalProceduralActionProfileSupport.containsAny(corpus,
                "ECA",
                "INFANCIA",
                "INFÂNCIA",
                "JUVENTUDE",
                "CRIANCA",
                "CRIANÇA",
                "ADOLESCENTE",
                "MENOR",
                "CONSELHO TUTELAR",
                "ACOLHIMENTO INSTITUCIONAL",
                "MEDIDA PROTETIVA",
                "ATO INFRACIONAL",
                "SOCIOEDUCATIVA",
                "ADOCAO",
                "ADOÇÃO");
    }
}
