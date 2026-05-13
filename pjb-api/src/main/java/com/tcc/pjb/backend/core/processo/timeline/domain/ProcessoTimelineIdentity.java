package com.tcc.pjb.backend.core.processo.timeline.domain;

import java.util.List;
import java.util.Objects;

public record ProcessoTimelineIdentity(
        Long processoId,
        String numeroProcesso,
        String ramoDireito,
        String rito,
        String fase,
        String status,
        String tribunal,
        String unidade,
        List<String> marcadores
) {
    public ProcessoTimelineIdentity {
        Objects.requireNonNull(processoId);
        numeroProcesso = numeroProcesso == null ? "NAO_INFORMADO" : numeroProcesso;
        ramoDireito = ramoDireito == null ? "NAO_INFORMADO" : ramoDireito;
        rito = rito == null ? "NAO_INFORMADO" : rito;
        fase = fase == null ? "NAO_INFORMADO" : fase;
        status = status == null ? "NAO_INFORMADO" : status;
        tribunal = tribunal == null ? "NAO_INFORMADO" : tribunal;
        unidade = unidade == null ? "NAO_INFORMADO" : unidade;
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
    }


    public ProcessoTimelineIdentity(Long processoId, String numeroProcesso) {
        this(processoId, numeroProcesso, null, null, null, null, null, null, List.of());
    }
}
