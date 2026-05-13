package com.tcc.pjb.backend.core.processo.lifecycle;

import java.util.List;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualDescriptor;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;

public record ProcessoLifecycleDecision(
        ProcessoLifecycleAction action,
        FaseProcessual faseOrigem,
        StatusProcesso statusOrigem,
        FaseProcessual faseDestino,
        StatusProcesso statusDestino,
        boolean permitida,
        String motivo,
        String responsavelSugerido,
        AtoProcessualDescriptor atoProcessual,
        List<String> alertas
) {

    public ProcessoLifecycleDecision {
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
    }

    public boolean hasWarnings() {
        return !alertas.isEmpty();
    }

    public boolean isRecursalTransition() {
        return action != null && action.isRecursal();
    }

    public boolean isTerminalTransition() {
        return action != null && action.isTerminal() && permitida;
    }

    public boolean requiresMagistrature() {
        return action != null && action.requiresMagistrature();
    }

    public String transitionKey() {
        String fromPhase = faseOrigem != null ? faseOrigem.name() : "SEM_FASE";
        String toPhase = faseDestino != null ? faseDestino.name() : "SEM_DESTINO";
        String fromStatus = statusOrigem != null ? statusOrigem.name() : "SEM_STATUS";
        String toStatus = statusDestino != null ? statusDestino.name() : "SEM_STATUS_DESTINO";
        return fromPhase + ">" + toPhase + "|" + fromStatus + ">" + toStatus;
    }
}
