package com.tcc.pjb.backend.core.processo.busca.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoBuscaCard(
        Long processoId,
        String numeroProcesso,
        String tribunal,
        String unidade,
        String comarca,
        String uf,
        String ramo,
        String rito,
        String fase,
        String status,
        String accentColor,
        boolean urgente,
        List<String> marcadores,
        Instant atualizadoEm
) {
    public ProcessoBuscaCard {
        Objects.requireNonNull(processoId);
        numeroProcesso = numeroProcesso == null ? "NAO_INFORMADO" : numeroProcesso;
        tribunal = tribunal == null ? "NAO_INFORMADO" : tribunal;
        unidade = unidade == null ? "NAO_INFORMADO" : unidade;
        comarca = comarca == null ? "NAO_INFORMADO" : comarca;
        uf = uf == null ? "NAO_INFORMADO" : uf;
        ramo = ramo == null ? "NAO_INFORMADO" : ramo;
        rito = rito == null ? "NAO_INFORMADO" : rito;
        fase = fase == null ? "NAO_INFORMADO" : fase;
        status = status == null ? "NAO_INFORMADO" : status;
        accentColor = accentColor == null ? "slate" : accentColor;
        marcadores = marcadores == null ? List.of() : List.copyOf(marcadores);
    }
}
