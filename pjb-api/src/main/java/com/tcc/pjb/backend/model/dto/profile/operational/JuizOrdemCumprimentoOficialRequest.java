package com.tcc.pjb.backend.model.dto.profile.operational;

import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public record JuizOrdemCumprimentoOficialRequest(
        Long oficialId,
        @NotBlank String fundamento,
        String conteudoOperacional,
        String tipoCumprimento,
        Integer prioridade,
        Instant dueAt,
        String janelaTerritorial,
        String bairroPreferencial,
        String microterritorio,
        Boolean cienciaObrigatoria,
        Boolean exigirOficioOriginalNoEncerramento,
        String observacao
) {
    public String fundamentoResolvido() {
        return normalize(fundamento, null);
    }

    public String conteudoOperacionalResolvido() {
        return normalize(conteudoOperacional, "Cumprimento determinado judicialmente no PJB.");
    }

    public String tipoCumprimentoResolvido() {
        return normalize(tipoCumprimento, "CUMPRIMENTO_JUDICIAL");
    }

    public int prioridadeResolvida() {
        if (prioridade == null) {
            return 1;
        }
        return Math.max(0, Math.min(prioridade, 5));
    }

    public Instant dueAtResolvido() {
        return dueAt;
    }

    public String janelaTerritorialResolvida() {
        return normalize(janelaTerritorial, null);
    }

    public String bairroPreferencialResolvido() {
        return normalize(bairroPreferencial, null);
    }

    public String microterritorioResolvido() {
        return normalize(microterritorio, null);
    }

    public boolean cienciaObrigatoriaResolvida() {
        return !Boolean.FALSE.equals(cienciaObrigatoria);
    }

    public boolean exigirOficioOriginalNoEncerramentoResolvido() {
        return !Boolean.FALSE.equals(exigirOficioOriginalNoEncerramento);
    }

    public String observacaoResolvida() {
        return normalize(observacao, null);
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
