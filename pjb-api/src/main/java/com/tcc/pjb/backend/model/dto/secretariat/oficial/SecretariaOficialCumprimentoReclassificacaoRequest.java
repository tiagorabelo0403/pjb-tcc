package com.tcc.pjb.backend.model.dto.secretariat.oficial;

import jakarta.validation.constraints.Size;

public record SecretariaOficialCumprimentoReclassificacaoRequest(
        @Size(max = 40) String classificacao,
        @Size(max = 1200) String observacao,
        Boolean manterPrioridadeOriginal,
        Boolean concluirDeskOriginal
) {
    public String classificacaoResolvida() {
        if (classificacao == null || classificacao.isBlank()) {
            return null;
        }
        return classificacao.trim().toUpperCase();
    }

    public String observacaoResolvida() {
        if (observacao == null) {
            return null;
        }
        String normalized = observacao.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public boolean manterPrioridadeOriginalResolvido() {
        return !Boolean.FALSE.equals(manterPrioridadeOriginal);
    }

    public boolean concluirDeskOriginalResolvido() {
        return !Boolean.FALSE.equals(concluirDeskOriginal);
    }
}
