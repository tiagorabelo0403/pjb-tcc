package com.tcc.pjb.backend.core.processo.operacao.domain;

import java.util.List;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import java.util.Objects;

public record ProcessoOperacaoFaixa(
        String codigo,
        String titulo,
        String eixo,
        String estado,
        double saturacao,
        long bloqueios,
        List<String> detalhes,
        List<String> acoesImediatas
) {
    public ProcessoOperacaoFaixa {
        Objects.requireNonNull(codigo);
        Objects.requireNonNull(titulo);
        eixo = eixo == null ? "OPERACAO" : eixo;
        estado = estado == null ? "STABLE" : estado;
        detalhes = PayloadMaps.copyDistinctStrings(detalhes);
        acoesImediatas = PayloadMaps.copyDistinctStrings(acoesImediatas);
    }
}
