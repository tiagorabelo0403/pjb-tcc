package com.tcc.pjb.backend.service.offline.domain;

import java.util.List;
import java.util.Map;

public record SincronizarBundleRequest(List<Map<String, Object>> acoes,
                                       String deviceClock,
                                       String ultimaSincronizacaoConhecida,
                                       String conflitoResumo) {
}
