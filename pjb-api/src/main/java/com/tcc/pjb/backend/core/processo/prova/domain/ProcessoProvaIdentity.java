package com.tcc.pjb.backend.core.processo.prova.domain;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.Objects;
import java.util.UUID;

public record ProcessoProvaIdentity(
        Long processoId,
        String numeroProcesso,
        UUID documentoId,
        String nomeDocumento,
        String contentType,
        NivelSigilo nivelSigilo
) {
    public ProcessoProvaIdentity {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        nomeDocumento = Objects.toString(nomeDocumento, "").trim();
        contentType = Objects.toString(contentType, "").trim().toLowerCase();
        nivelSigilo = nivelSigilo == null ? NivelSigilo.PUBLICO : nivelSigilo;
    }


    public String tituloDocumento() {
        return nomeDocumento;
    }
}
