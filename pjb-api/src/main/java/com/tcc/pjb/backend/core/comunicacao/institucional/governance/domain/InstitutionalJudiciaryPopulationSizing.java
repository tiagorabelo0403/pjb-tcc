package com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain;

import java.time.Instant;
import java.util.List;

public record InstitutionalJudiciaryPopulationSizing(
        int tribunaisNacionais,
        int magistradosAtivosBaseline,
        int servidoresAtivosBaseline,
        int usuariosInternosCoreBaseline,
        int afiliacoesInstitucionaisAtivasModeladas,
        int nomeacoesAtivasModeladas,
        int contextosInstitucionaisAtivosModelados,
        int picoSessoesConcorrentesPlanejado,
        int replicasLeituraRegionaisMinimas,
        int bucketsParticionamentoEscritaMinimos,
        List<String> eixosParticionamento,
        List<String> segmentosInstitucionaisCobertos,
        List<String> fundamentos,
        Instant generatedAt
) {
    public InstitutionalJudiciaryPopulationSizing {
        eixosParticionamento = eixosParticionamento == null ? List.of() : List.copyOf(eixosParticionamento);
        segmentosInstitucionaisCobertos = segmentosInstitucionaisCobertos == null ? List.of() : List.copyOf(segmentosInstitucionaisCobertos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
