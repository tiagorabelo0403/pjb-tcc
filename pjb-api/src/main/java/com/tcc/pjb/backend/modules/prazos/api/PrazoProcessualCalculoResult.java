package com.tcc.pjb.backend.modules.prazos.api;

import java.time.LocalDate;
import java.util.List;

public record PrazoProcessualCalculoResult(
        LocalDate dataInicio,
        LocalDate vencimentoNacional,
        LocalDate vencimentoForense,
        int diasCorridos,
        int diasUteisNacionais,
        int diasUteisForenses,
        String tipoPrazo,
        String ramo,
        String grau,
        String tribunalCodigo,
        String uf,
        String comarca,
        boolean marcoInicialDiaUtil,
        String motivoMarcoInicial,
        List<String> advertencias,
        String fundamentoNacional,
        String fundamentoForense,
        boolean conferenciaManualRecomendada) {

    public PrazoProcessualCalculoResult {
        advertencias = advertencias == null ? List.of() : List.copyOf(advertencias);
    }

    public PrazoProcessualCalculoResult comConferenciaManual(boolean valor) {
        return new PrazoProcessualCalculoResult(
                dataInicio,
                vencimentoNacional,
                vencimentoForense,
                diasCorridos,
                diasUteisNacionais,
                diasUteisForenses,
                tipoPrazo,
                ramo,
                grau,
                tribunalCodigo,
                uf,
                comarca,
                marcoInicialDiaUtil,
                motivoMarcoInicial,
                advertencias,
                fundamentoNacional,
                fundamentoForense,
                valor
        );
    }
}
