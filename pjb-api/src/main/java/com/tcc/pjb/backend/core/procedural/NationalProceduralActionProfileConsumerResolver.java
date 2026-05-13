package com.tcc.pjb.backend.core.procedural;

import java.util.LinkedHashSet;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
class NationalProceduralActionProfileConsumerResolver {

    private final NationalProceduralActionProfileMessages messages;

    NationalProceduralActionProfileConsumerResolver(NationalProceduralActionProfileMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    NationalProceduralActionProfile resolve(NationalProceduralActionProfileContext context) {
        Objects.requireNonNull(context);
        String corpus = context.corpus() == null ? "" : context.corpus();
        if (NationalProceduralActionProfileSupport.containsAny(corpus, "CONSUMIDOR", "NEGATIVACAO", "COBRANCA INDEVIDA", "PLANO SAUDE", "TELEFONIA", "ENERGIA", "BANCARIO", "INDENIZACAO", "DANO MORAL", "OBRIGACAO FAZER")) {
            String nature = NationalProceduralActionProfileSupport.containsAny(corpus, "OBRIGACAO FAZER", "FORNECIMENTO", "ENTREGA")
                    ? "OBRIGACAO_DE_FAZER"
                    : NationalProceduralActionProfileSupport.containsAny(corpus, "COBRANCA", "REPETICAO INDEBITO")
                    ? "COBRANCA_REPETICAO"
                    : "INDENIZATORIA";
            return profile(context, nature, "CIVIL_CONSUMO", false, "COMUM_ORDINARIO", "CIVEL", messages.consumoReason(), messages.consumoLegalBase());
        }
        String defaultRito = context.canonical() != null && context.canonical().rito() != null
                ? context.canonical().rito().name()
                : "COMUM_ORDINARIO";
        return profile(context, "ACAO_CIVEL_GERAL", "CIVIL_GERAL", false, defaultRito, "CIVEL", messages.civelReason(), messages.civelLegalBase());
    }

    private static NationalProceduralActionProfile profile(NationalProceduralActionProfileContext context,
                                                           String actionNature,
                                                           String actionFamily,
                                                           boolean specialProcedure,
                                                           String defaultRito,
                                                           String varaFamily,
                                                           String reason,
                                                           String legalBase) {
        LinkedHashSet<String> markers = new LinkedHashSet<>();
        if (context.partyProfile() != null && context.partyProfile().publicParty()) {
            markers.add("LITIGIO_COM_PODER_PUBLICO");
        }
        return NationalProceduralActionProfileSupport.profile(
                actionNature,
                actionFamily,
                specialProcedure,
                defaultRito,
                varaFamily,
                markers,
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                reason,
                legalBase
        );
    }
}
