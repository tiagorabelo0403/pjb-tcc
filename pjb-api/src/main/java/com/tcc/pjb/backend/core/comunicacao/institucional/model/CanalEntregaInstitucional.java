package com.tcc.pjb.backend.core.comunicacao.institucional.model;

import java.util.Locale;
import com.tcc.pjb.backend.model.entity.enums.CanalComunicacaoInstitucional;

public record CanalEntregaInstitucional(
        CanalComunicacaoInstitucional canal,
        boolean principal,
        boolean exigeCienciaPessoal,
        int slaCienciaHoras,
        int slaRespostaHoras,
        String endpoint,
        String observacao
) {
    public CanalEntregaInstitucional {
        if (canal == null) {
            throw new IllegalArgumentException("canal é obrigatório");
        }
        slaCienciaHoras = normalizeSla(slaCienciaHoras, 48);
        slaRespostaHoras = normalizeSla(slaRespostaHoras, 120);
        endpoint = normalizeOptional(endpoint);
        observacao = normalizeOptional(observacao);
    }

    public boolean isCanalPrincipalJuridico() {
        return principal && canal.isPrincipalJuridico();
    }

    public boolean isCanalAviso() {
        return canal.isAvisoInformativo();
    }

    private static int normalizeSla(int value, int defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    private static String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.contains("://")) {
            return trimmed;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }
}
