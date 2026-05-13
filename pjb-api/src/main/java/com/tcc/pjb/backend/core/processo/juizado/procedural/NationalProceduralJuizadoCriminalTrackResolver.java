package com.tcc.pjb.backend.core.processo.juizado.procedural;

import java.util.LinkedHashSet;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class NationalProceduralJuizadoCriminalTrackResolver {

    private final NationalProceduralJuizadoDecisionMessages messages;

    public NationalProceduralJuizadoCriminalTrackResolver(NationalProceduralJuizadoDecisionMessages messages) {
        this.messages = Objects.requireNonNull(messages);
    }

    NationalProceduralJuizadoDecision resolve(NationalProceduralJuizadoDecisionContext context) {
        Objects.requireNonNull(context);
        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        LinkedHashSet<String> legalBases = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        reasons.add(messages.jecrimReason());
        legalBases.add(messages.jecrimLegalBase());
        reviewChecklist.add(messages.jecrimChecklist());
        return NationalProceduralJuizadoDecisionSupport.decision(true, "JUIZADO_ESPECIAL_CRIMINAL", reasons, legalBases, null, reviewChecklist, 0.63d, true);
    }
}
