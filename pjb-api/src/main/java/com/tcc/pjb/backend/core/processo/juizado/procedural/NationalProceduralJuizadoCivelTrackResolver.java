package com.tcc.pjb.backend.core.processo.juizado.procedural;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJuizadoCivelTrackResolver {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final NationalProceduralJuizadoDecisionMessages messages;

    public NationalProceduralJuizadoCivelTrackResolver(NationalProceduralJuizadoDecisionMessages messages) {
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
            alerts.add(messages.civelJuizadoValueMissingAlert());
            reviewChecklist.add(messages.civelJuizadoValueMissingChecklist());
            return NationalProceduralJuizadoDecisionSupport.decision(false, null, reasons, legalBases, alerts, reviewChecklist, 0.49d, true);
        }
        if (!context.teto().violacao() && NationalProceduralJuizadoDecisionSupport.containsAny(NationalProceduralJuizadoDecisionSupport.firstNonBlank(context.teto().ritoSugerido(), context.competence().ritoSugerido()), "JUIZADO_ESPECIAL_CIVEL", "JUIZADO_ESPECIAL")) {
            reasons.add(messages.civelJuizadoReason());
            legalBases.add(messages.civelJuizadoLegalBase());
            if (NationalProceduralJuizadoDecisionSupport.containsAny(corpus, "LAUDO PERICIAL COMPLEXO", "ENGENHARIA", "CONTABIL COMPLEXA", "CADEIA DOCUMENTAL MASSIVA")) {
                alerts.add(messages.civelJuizadoComplexEvidenceAlert());
                reviewChecklist.add(messages.civelJuizadoComplexEvidenceChecklist());
                return NationalProceduralJuizadoDecisionSupport.decision(true, "JUIZADO_ESPECIAL_CIVEL", reasons, legalBases, alerts, reviewChecklist, 0.66d, true);
            }
            return NationalProceduralJuizadoDecisionSupport.decision(true, "JUIZADO_ESPECIAL_CIVEL", reasons, legalBases, alerts, reviewChecklist, 0.90d, false);
        }
        return NationalProceduralJuizadoDecisionSupport.decision(false, null, reasons, legalBases, alerts, reviewChecklist, 0.78d, false);
    }
}
