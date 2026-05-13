package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class NationalProceduralActionProfileBusinessResolver {

    private final NationalProceduralActionProfileMessages messages;

    NationalProceduralActionProfileBusinessResolver(NationalProceduralActionProfileMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    Optional<NationalProceduralActionProfile> resolve(NationalProceduralActionProfileContext context) {
        Objects.requireNonNull(context);
        String corpus = context.corpus() == null ? "" : context.corpus();
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "MONITORIA", "CHEQUE", "NOTA PROMISSORIA", "DUPLICATA")) {
            return Optional.of(profile("MONITORIA", "CIVIL_CREDITOS", true, "CIVIL_ACAO_MONITORIA", "MONITORIA", messages.civelReason(), messages.civelLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "CONSIGNACAO PAGAMENTO", "DEPOSITO JUDICIAL PARA QUITACAO")) {
            return Optional.of(profile("CONSIGNACAO_EM_PAGAMENTO", "CIVIL_OBRIGACOES", true, "CIVIL_CONSIGNACAO_PAGAMENTO", "OBRIGACOES", messages.civelReason(), messages.civelLegalBase()));
        }
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "RECUPERACAO JUDICIAL", "PLANO RECUPERACAO", "RECUPERACAO EXTRAJUDICIAL", "FALENCIA")) {
            String rito = NationalProceduralActionProfileSupport.containsAny(corpus, "FALENCIA")
                    ? "FALENCIA"
                    : NationalProceduralActionProfileSupport.containsAny(corpus, "EXTRAJUDICIAL")
                    ? "RECUPERACAO_EXTRAJUDICIAL"
                    : "RECUPERACAO_JUDICIAL";
            return Optional.of(profile("INSOLVENCIA_EMPRESARIAL", "EMPRESARIAL", true, rito, "EMPRESARIAL", messages.empresarialReason(), messages.empresarialLegalBase()));
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
