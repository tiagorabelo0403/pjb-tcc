package com.tcc.pjb.backend.model.dto.profile.operational;

public record SecretariaPublicacaoAcordaoRequest(
        String numeroAcordao,
        String ementaResumo,
        String inteiroTeorRef,
        Boolean gerarBaixaOrigem
) {
    public String numeroAcordaoResolvido() {
        return normalize(numeroAcordao);
    }

    public String ementaResumoResolvido() {
        return normalize(ementaResumo);
    }

    public String inteiroTeorRefResolvido() {
        return normalize(inteiroTeorRef);
    }

    public boolean gerarBaixaOrigemResolvida() {
        return Boolean.TRUE.equals(gerarBaixaOrigem);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
