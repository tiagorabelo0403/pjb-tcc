package com.tcc.pjb.backend.model.dto.secretariat.oficial;

import java.time.Instant;

public record SecretariaOficialCumprimentoMaterializacaoRequest(
        String ato,
        String observacao,
        String tipoDocumento,
        String descricao,
        String origem,
        String prazo,
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
        Boolean concluirDeskOrigem,
        Boolean manterRetornoForumAberto
) {

    public String atoResolvido(String fallback) {
        return normalize(ato, fallback);
    }

    public String observacaoResolvida() {
        return normalize(observacao, null);
    }

    public String tipoDocumentoResolvido(String fallback) {
        return normalize(tipoDocumento, fallback);
    }

    public String descricaoResolvida(String fallback) {
        return normalize(descricao, fallback);
    }

    public String origemResolvida(String fallback) {
        return normalize(origem, fallback);
    }

    public String prazoResolvido(String fallback) {
        return normalize(prazo, fallback);
    }

    public Long oficialIdResolvido(Long fallback) {
        return oficialId != null ? oficialId : fallback;
    }

    public String fundamentoResolvido(String fallback) {
        return normalize(fundamento, fallback);
    }

    public String conteudoOperacionalResolvido(String fallback) {
        return normalize(conteudoOperacional, fallback);
    }

    public String conteudoOperacionalResolvida(String fallback) {
        return conteudoOperacionalResolvido(fallback);
    }

    public String tipoCumprimentoResolvido(String fallback) {
        return normalize(tipoCumprimento, fallback);
    }

    public int prioridadeResolvida(int fallback) {
        if (prioridade == null) {
            return clampPriority(fallback);
        }
        return clampPriority(prioridade);
    }

    public Instant dueAtResolvido(Instant fallback) {
        return dueAt != null ? dueAt : fallback;
    }

    public String janelaTerritorialResolvida(String fallback) {
        return normalize(janelaTerritorial, fallback);
    }

    public String bairroPreferencialResolvida(String fallback) {
        return normalize(bairroPreferencial, fallback);
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

    public boolean concluirDeskOrigemResolvido() {
        return !Boolean.FALSE.equals(concluirDeskOrigem);
    }

    public boolean manterRetornoForumAbertoResolvido(boolean fallback) {
        return manterRetornoForumAberto != null ? manterRetornoForumAberto : fallback;
    }

    private static int clampPriority(int value) {
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
