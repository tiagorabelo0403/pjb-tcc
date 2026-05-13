package com.tcc.pjb.backend.core.processo.runtime.application;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ProcessoMalhaEntradaCanonicalizer {
    private final ProcessoMalhaContractGuard guard = new ProcessoMalhaContractGuard();

    public Map<String, Object> canonicalizar(Long processoId, RamoDireito ramoDireito, String numeroProcesso, String numeroUnificado, String papel) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("processoId", guard.requireProcessoId(processoId));
        resultado.put("ramoDireito", guard.ramoOuPadrao(ramoDireito, RamoDireito.CIVIL));
        resultado.put("numeroProcesso", guard.canonicalizeNumeroProcesso(numeroProcesso));
        resultado.put("numeroUnificado", guard.canonicalizeNumeroProcesso(numeroUnificado));
        resultado.put("papel", guard.canonicalize(papel));
        return Map.copyOf(resultado);
    }
}
