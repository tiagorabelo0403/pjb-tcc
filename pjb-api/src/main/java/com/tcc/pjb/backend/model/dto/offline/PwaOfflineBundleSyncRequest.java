package com.tcc.pjb.backend.model.dto.offline;

import java.util.List;
import java.util.Map;

public record PwaOfflineBundleSyncRequest(
        List<Map<String, Object>> acoes,
        String deviceClock,
        String ultimaSincronizacaoConhecida,
        String conflitoResumo
) {
}
