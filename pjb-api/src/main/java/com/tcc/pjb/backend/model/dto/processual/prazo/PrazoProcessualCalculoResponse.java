package com.tcc.pjb.backend.model.dto.processual.prazo;

import java.time.LocalDate;
import java.util.List;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;

public record PrazoProcessualCalculoResponse(
        LocalDate dataInicio,
        LocalDate vencimentoNacional,
        LocalDate vencimentoForense,
        int diasCorridos,
        int diasUteisNacionais,
        int diasUteisForenses,
        NationalPrazoEngine.TipoPrazo tipoPrazo,
        RamoDireito ramo,
        GrauJurisdicao grau,
        String tribunalCodigo,
        String uf,
        String comarca,
        boolean marcoInicialDiaUtil,
        String motivoMarcoInicial,
        List<String> advertencias,
        String fundamentoNacional,
        String fundamentoForense) {
}
