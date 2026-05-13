package com.tcc.pjb.backend.model.dto.profile.operational;

import java.time.Instant;

public record JuizGabineteOficialRetornoDecisaoRequest(
        Long oficialId,
        String fundamento,
        String conteudoOperacional,
        String tipoCumprimento,
        Integer prioridade,
        Instant dueAt,
        String janelaTerritorial,
        String bairroPreferencial,
        String microterritorio,
        Boolean cienciaObrigatoria,
        Boolean exigirOficioOriginalNoEncerramento,
        String observacao,
        Boolean concluirItemOrigem
) {

    public Long oficialIdResolvido(Long fallback) {
        return oficialId != null ? oficialId : fallback;
    }

    public String fundamentoResolvido(String fallback) {
        return normalize(fundamento, fallback);
    }

    public String conteudoOperacionalResolvido(String fallback) {
        return normalize(conteudoOperacional, fallback);
    }

    public String tipoCumprimentoResolvido(String fallback) {
        return normalize(tipoCumprimento, fallback);
    }

    public int prioridadeResolvida(int fallback) {
        if (prioridade == null) {
            return clamp(fallback);
        }
        return clamp(prioridade);
    }

    public Instant dueAtResolvido(Instant fallback) {
        return dueAt != null ? dueAt : fallback;
    }

    public String janelaTerritorialResolvida(String fallback) {
        return normalize(janelaTerritorial, fallback);
    }

    public String bairroPreferencialResolvido(String fallback) {
        return normalize(bairroPreferencial, fallback);
    }

    public String bairroPreferencialResolvida(String fallback) {
        return bairroPreferencialResolvido(fallback);
    }

    public String microterritorioResolvido(String fallback) {
        return normalize(microterritorio, fallback);
    }

    public String microterritorioResolvida(String fallback) {
        return microterritorioResolvido(fallback);
    }

    public boolean cienciaObrigatoriaResolvida(boolean fallback) {
        return cienciaObrigatoria != null ? cienciaObrigatoria : fallback;
    }

    public boolean exigirOficioOriginalNoEncerramentoResolvido(boolean fallback) {
        return exigirOficioOriginalNoEncerramento != null ? exigirOficioOriginalNoEncerramento : fallback;
    }

    public String observacaoResolvida() {
        return normalize(observacao, null);
    }

    public boolean concluirItemOrigemResolvido(boolean fallback) {
        return concluirItemOrigem != null ? concluirItemOrigem : fallback;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(value, 5));
    }

    private static String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? fallback : normalized;
    }
}
