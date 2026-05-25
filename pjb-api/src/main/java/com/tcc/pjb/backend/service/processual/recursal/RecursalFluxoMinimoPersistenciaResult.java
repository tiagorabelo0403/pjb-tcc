package com.tcc.pjb.backend.service.processual.recursal;

import com.tcc.pjb.backend.core.kernel.recursal.mesh.RecursalLifecycleState;
import com.tcc.pjb.backend.integration.mni.domain.MniStatusRemessa;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public record RecursalFluxoMinimoPersistenciaResult(
        String recursoId,
        String numeroRecursal,
        UUID documentoId,
        String documentoHash,
        RecursalLifecycleState estadoMalha,
        Long remessaId,
        MniStatusRemessa remessaStatus,
        String tribunalDestino) {

    public Map<String, Object> toMap() {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("recursoId", recursoId);
        out.put("numeroRecursal", numeroRecursal);
        out.put("documentoId", documentoId);
        out.put("documentoHash", documentoHash);
        out.put("estadoMalha", estadoMalha == null ? null : estadoMalha.name());
        out.put("remessaId", remessaId);
        out.put("remessaStatus", remessaStatus == null ? null : remessaStatus.name());
        out.put("tribunalDestino", tribunalDestino);
        return Map.copyOf(out);
    }
}
