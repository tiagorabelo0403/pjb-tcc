package com.tcc.pjb.backend.core.processo.operacao.domain;

import java.util.List;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.Objects;

public record ProcessoOperacaoIdentity(
        Long processoId,
        String numeroProcesso,
        String tribunal,
        String unidade,
        String ramo,
        String rito,
        String fase,
        String status,
        List<String> marcadores
) {
    public ProcessoOperacaoIdentity {
        Objects.requireNonNull(processoId);
        numeroProcesso = numeroProcesso == null ? "NAO_INFORMADO" : numeroProcesso;
        tribunal = tribunal == null ? "NAO_INFORMADO" : tribunal;
        unidade = unidade == null ? "NAO_INFORMADO" : unidade;
        ramo = ramo == null ? "NAO_INFORMADO" : ramo;
        rito = rito == null ? "NAO_INFORMADO" : rito;
        fase = fase == null ? "NAO_INFORMADO" : fase;
        status = status == null ? "NAO_INFORMADO" : status;
        marcadores = PayloadMaps.copyDistinctStrings(marcadores);
    }
}
