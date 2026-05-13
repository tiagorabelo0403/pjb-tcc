package com.tcc.pjb.backend.core.distribuicao;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.List;

public record PjbDistribuicaoContext(
        RitoProcessual ritoCanônico,
        String tribunalCode,
        String comarcaCode,
        String unidadeCode,
        String competencia,
        BigDecimal valorCausa,
        String materia,
        GrauJurisdicao grau,
        boolean juizo100Digital,
        boolean nucleo40Disponivel,
        boolean sigilo,
        boolean urgente,
        int complexidadeProbatoria,
        List<String> sinaisProcessuais
) {

    public List<String> safeSinais() {
        return sinaisProcessuais == null ? List.of() : List.copyOf(sinaisProcessuais);
    }
}
