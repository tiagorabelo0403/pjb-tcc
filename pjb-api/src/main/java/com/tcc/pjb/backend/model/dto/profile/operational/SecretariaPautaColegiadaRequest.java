package com.tcc.pjb.backend.model.dto.profile.operational;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record SecretariaPautaColegiadaRequest(
        @NotNull LocalDateTime pautaDataHora,
        GrauJurisdicao grau,
        String tribunalSigla,
        String orgaoJulgador,
        String relatorNome,
        String revisorNome,
        Boolean sessaoVirtual,
        String canalSessao
) {
    public boolean sessaoVirtualResolvida() {
        return Boolean.TRUE.equals(sessaoVirtual);
    }

    public String canalSessaoResolvido() {
        return normalize(canalSessao);
    }

    public String tribunalSiglaResolvida() {
        return normalize(tribunalSigla);
    }

    public String orgaoJulgadorResolvido() {
        return normalize(orgaoJulgador);
    }

    public String relatorNomeResolvido() {
        return normalize(relatorNome);
    }

    public String relatorNomeResolvida() {
        return relatorNomeResolvido();
    }

    public String revisorNomeResolvido() {
        return normalize(revisorNome);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
