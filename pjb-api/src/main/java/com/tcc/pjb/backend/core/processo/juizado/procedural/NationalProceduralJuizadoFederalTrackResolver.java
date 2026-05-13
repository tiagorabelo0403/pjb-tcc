package com.tcc.pjb.backend.core.processo.juizado.procedural;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJuizadoFederalTrackResolver {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final NationalProceduralJuizadoDecisionMessages messages;

    public NationalProceduralJuizadoFederalTrackResolver(NationalProceduralJuizadoDecisionMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    NationalProceduralJuizadoDecision resolve(NationalProceduralJuizadoDecisionContext context) {
        Objects.requireNonNull(context);
        BigDecimal valor = NationalProceduralJuizadoDecisionSupport.decimal(context.payload() == null ? null : context.payload().get("valorCausa"));
        String corpus = context.corpus() == null ? "" : context.corpus();
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        LinkedHashSet<String> legalBases = new LinkedHashSet<>();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (valor.compareTo(ZERO) <= 0) {
            alerts.add(messages.federalJuizadoValueMissingAlert());
            reviewChecklist.add(messages.federalJuizadoValueMissingChecklist());
            return NationalProceduralJuizadoDecisionSupport.decision(false, null, reasons, legalBases, alerts, reviewChecklist, 0.52d, true);
        }
        if (!context.teto().violacao() && NationalProceduralJuizadoDecisionSupport.containsAny(NationalProceduralJuizadoDecisionSupport.firstNonBlank(context.teto().ritoSugerido(), context.competence().ritoSugerido()), "JUIZADO_ESPECIAL_FEDERAL")) {
            reasons.add(messages.federalJuizadoReason());
            legalBases.add(messages.federalJuizadoLegalBase());
            if (NationalProceduralJuizadoDecisionSupport.containsAny(corpus, "PERICIA COMPLEXA", "ENGENHARIA COMPLEXA", "CONTABIL COMPLEXA")) {
                alerts.add(messages.federalJuizadoComplexEvidenceAlert());
                reviewChecklist.add(messages.federalJuizadoComplexEvidenceChecklist());
                return NationalProceduralJuizadoDecisionSupport.decision(true, "JUIZADO_ESPECIAL_FEDERAL", reasons, legalBases, alerts, reviewChecklist, 0.68d, true);
            }
            return NationalProceduralJuizadoDecisionSupport.decision(true, "JUIZADO_ESPECIAL_FEDERAL", reasons, legalBases, alerts, reviewChecklist, 0.91d, false);
        }
        return NationalProceduralJuizadoDecisionSupport.decision(false, null, reasons, legalBases, alerts, reviewChecklist, 0.86d, context.teto().bloqueante());
    }
}
