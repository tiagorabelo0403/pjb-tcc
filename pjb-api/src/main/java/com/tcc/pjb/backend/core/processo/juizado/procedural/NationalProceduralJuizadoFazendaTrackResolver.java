package com.tcc.pjb.backend.core.processo.juizado.procedural;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJuizadoFazendaTrackResolver {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final NationalProceduralJuizadoDecisionMessages messages;

    public NationalProceduralJuizadoFazendaTrackResolver(NationalProceduralJuizadoDecisionMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    NationalProceduralJuizadoDecision resolve(NationalProceduralJuizadoDecisionContext context) {
        Objects.requireNonNull(context);
        BigDecimal valor = NationalProceduralJuizadoDecisionSupport.decimal(context.payload() == null ? null : context.payload().get("valorCausa"));
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        LinkedHashSet<String> legalBases = new LinkedHashSet<>();
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        if (valor.compareTo(ZERO) <= 0) {
            alerts.add(messages.fazendaJuizadoValueMissingAlert());
            reviewChecklist.add(messages.fazendaJuizadoValueMissingChecklist());
            return NationalProceduralJuizadoDecisionSupport.decision(false, null, reasons, legalBases, alerts, reviewChecklist, 0.51d, true);
        }
        if (!context.teto().violacao() && NationalProceduralJuizadoDecisionSupport.containsAny(NationalProceduralJuizadoDecisionSupport.firstNonBlank(context.teto().ritoSugerido(), context.competence().ritoSugerido()), "JUIZADO_ESPECIAL_FAZENDA_PUBLICA")) {
            reasons.add(messages.fazendaJuizadoReason());
            legalBases.add(messages.fazendaJuizadoLegalBase());
            return NationalProceduralJuizadoDecisionSupport.decision(true, "JUIZADO_ESPECIAL_FAZENDA_PUBLICA", reasons, legalBases, alerts, reviewChecklist, 0.90d, false);
        }
        return NationalProceduralJuizadoDecisionSupport.decision(false, null, reasons, legalBases, alerts, reviewChecklist, 0.84d, context.teto().bloqueante());
    }
}
