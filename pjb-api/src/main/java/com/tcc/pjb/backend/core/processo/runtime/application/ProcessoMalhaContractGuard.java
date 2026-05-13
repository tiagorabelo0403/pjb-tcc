package com.tcc.pjb.backend.core.processo.runtime.application;

import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import java.util.Locale;

public final class ProcessoMalhaContractGuard {
    public Long requireProcessoId(Long processoId) {
        if (processoId == null || processoId <= 0L) {
            throw new IllegalArgumentException("processoId invalido");
        }
        return processoId;
    }

    public String requireTexto(String valor, String campo) {
        String normalizado = canonicalize(valor);
        if (normalizado.isBlank()) {
            throw new IllegalArgumentException(campo + " invalido");
        }
        return normalizado;
    }

    public RamoDireito ramoOuPadrao(RamoDireito ramoDireito, RamoDireito padrao) {
        return ramoDireito != null ? ramoDireito : padrao;
    }

    public String canonicalize(String valor) {
        if (valor == null) {
            return "";
        }
        String normalizado = valor
                .strip()
                .replace('\u00A0', ' ')
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
        return normalizado;
    }

    public String canonicalizeNumeroProcesso(String valor) {
        return canonicalize(valor).replaceAll("[^0-9]", "");
    }
}
