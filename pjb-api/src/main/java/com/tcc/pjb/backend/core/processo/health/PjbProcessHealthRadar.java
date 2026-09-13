package com.tcc.pjb.backend.core.processo.health;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public final class PjbProcessHealthRadar {

    public PjbProcessHealthSnapshot assess(List<PjbProcessHealthSignal> signals) {
        List<PjbProcessHealthSignal> normalized = signals == null ? List.of() : List.copyOf(signals);
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        double score = 0.96d;
        boolean blocking = false;
        for (PjbProcessHealthSignal signal : normalized) {
            score -= penalty(signal);
            blocking = blocking || signal.blocking() || "CRITICAL".equals(signal.severity());
            recommendedAction(signal, actions);
        }
        if (actions.isEmpty()) {
            actions.add("manter acompanhamento ordinário sem intervenção operacional adicional");
        }
        String status = blocking ? "BLOCKING_RISK" : normalized.isEmpty() ? "HEALTHY" : score < 0.72d ? "WATCHLIST" : "STABLE";
        return new PjbProcessHealthSnapshot(status, round(score), blocking, normalized, new ArrayList<>(actions));
    }

    private double penalty(PjbProcessHealthSignal signal) {
        if (signal == null) {
            return 0.02d;
        }
        return switch (signal.severity()) {
            case "CRITICAL" -> 0.28d;
            case "HIGH" -> 0.16d;
            case "MEDIUM" -> 0.08d;
            default -> 0.03d;
        };
    }

    private void recommendedAction(PjbProcessHealthSignal signal, LinkedHashSet<String> actions) {
        if (signal == null) {
            return;
        }
        switch (signal.type()) {
            case STAGNATION -> actions.add("revisar fila, responsável e último ato útil do processo");
            case DEADLINE_RISK -> actions.add("recalcular prazo, validar feriados e priorizar ato pendente");
            case NULLITY_RISK -> actions.add("submeter ato à revisão humana antes de assinatura ou publicação");
            case COMPETENCE_RISK -> actions.add("revalidar competência, prevenção, conexão e unidade destino");
            case SECRECY_RISK -> actions.add("rever envelopes de sigilo, RLS e superfície pública antes de exposição");
            case NOTICE_FAILURE -> actions.add("reemitir comunicação com cadeia de entrega verificável");
            case DOCUMENT_INTEGRITY_RISK -> actions.add("revalidar hash, assinatura, carimbo temporal e versão pública do documento");
            case RECUSAL_RISK -> actions.add("revalidar tempestividade, preparo, dialeticidade e trilha recursal");
            case QUEUE_BACKLOG -> actions.add("redistribuir lote operacional ou aplicar prioridade por urgência");
            case PRIORITY_RIGHT -> actions.add("aplicar tramitação prioritária e materializar justificativa auditável");
        }
    }

    private double round(double value) {
        return BigDecimal.valueOf(Math.max(0.0d, Math.min(1.0d, value))).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
