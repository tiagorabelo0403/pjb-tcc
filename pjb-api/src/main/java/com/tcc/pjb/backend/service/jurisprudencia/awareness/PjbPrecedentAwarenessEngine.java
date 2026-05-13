package com.tcc.pjb.backend.service.jurisprudencia.awareness;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class PjbPrecedentAwarenessEngine {

    public PjbPrecedentAwarenessReport evaluate(List<PjbPrecedentSignal> signals) {
        List<PjbPrecedentSignal> safeSignals = signals == null ? List.of() : List.copyOf(signals);
        boolean binding = safeSignals.stream().anyMatch(PjbPrecedentSignal::binding);
        boolean suspend = safeSignals.stream().anyMatch(PjbPrecedentSignal::suspensionRecommended);
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        for (PjbPrecedentSignal signal : safeSignals) {
            if (signal == null) {
                continue;
            }
            switch (signal.type()) {
                case REPERCUSSAO_GERAL, RECURSO_REPETITIVO, IRDR, IAC -> actions.add("avaliar sobrestamento, aderência e distinção fundamentada");
                case SUMULA -> actions.add("materializar aplicação ou afastamento da súmula com trilha explicável");
                case DISTINGUISHING -> actions.add("submeter distinguishing à revisão humana antes da minuta");
                case OVERRULING_RISK -> actions.add("validar atualidade do precedente antes de decisão ou recurso");
            }
        }
        if (actions.isEmpty()) {
            actions.add("manter observação ordinária de precedentes qualificados");
        }
        String status = suspend ? "SUSPENSION_REVIEW_REQUIRED" : binding ? "BINDING_PRECEDENT_REVIEW" : safeSignals.isEmpty() ? "NO_SIGNAL" : "ADVISORY_ONLY";
        return new PjbPrecedentAwarenessReport(status, binding, suspend, safeSignals, new ArrayList<>(actions));
    }
}
