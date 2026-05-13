package com.tcc.pjb.backend.core.processo.evidencia.domain;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record ProcessoEvidenciaItem(
        Long processoId,
        String numeroProcesso,
        UUID documentoId,
        String nomeDocumento,
        String sha256,
        NivelSigilo nivelSigilo,
        String relacao,
        double score,
        List<String> fundamentos
) {
    public ProcessoEvidenciaItem {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        nomeDocumento = Objects.toString(nomeDocumento, "").trim();
        sha256 = Objects.toString(sha256, "").trim().toLowerCase();
        nivelSigilo = nivelSigilo == null ? NivelSigilo.PUBLICO : nivelSigilo;
        relacao = Objects.toString(relacao, "").trim();
        score = Math.max(0d, Math.min(1d, score));
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
