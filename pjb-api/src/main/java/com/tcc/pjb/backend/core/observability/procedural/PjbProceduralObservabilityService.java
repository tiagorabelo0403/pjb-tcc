package com.tcc.pjb.backend.core.observability.procedural;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class PjbProceduralObservabilityService {

    public PjbProceduralObservabilitySnapshot summarize(List<PjbProceduralObservation> observations) {
        List<PjbProceduralObservation> safe = observations == null ? List.of() : List.copyOf(observations);
        EnumMap<PjbProceduralObservationType, Long> counts = new EnumMap<>(PjbProceduralObservationType.class);
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        int critical = 0;
        for (PjbProceduralObservation observation : safe) {
            if (observation == null || observation.type() == null) {
                continue;
            }
            counts.merge(observation.type(), 1L, Long::sum);
            if (observation.severity() >= 8) {
                critical++;
            }
            action(observation.type(), actions);
        }
        if (actions.isEmpty()) {
            actions.add("manter telemetria processual ordinária");
        }
        String status = critical > 0 ? "CRITICAL_PROCEDURAL_PRESSURE" : safe.isEmpty() ? "NO_SIGNAL" : "OBSERVED";
        return new PjbProceduralObservabilitySnapshot(status, critical, Map.copyOf(counts), new ArrayList<>(actions));
    }

    private void action(PjbProceduralObservationType type, LinkedHashSet<String> actions) {
        switch (type) {
            case STALLED_PROCESS -> actions.add("priorizar processo parado e identificar último ato útil");
            case QUEUE_SPIKE, SECRETARIAT_OVERLOAD -> actions.add("redistribuir carga operacional por unidade e perfil");
            case NOTICE_FAILURE -> actions.add("reprocessar comunicação com cadeia de entrega verificável");
            case SIGNATURE_LATENCY -> actions.add("acionar contingência de assinatura e carimbo temporal");
            case PROTOCOL_BACKLOG -> actions.add("abrir replay de protocolo com caixa-preta operacional");
            case DEADLINE_PRESSURE -> actions.add("reavaliar prazos fatais e indisponibilidades relacionadas");
        }
    }
}
