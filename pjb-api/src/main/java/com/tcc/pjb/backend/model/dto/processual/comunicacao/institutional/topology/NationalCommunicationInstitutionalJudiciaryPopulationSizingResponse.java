package com.tcc.pjb.backend.model.dto.processual.comunicacao.institutional.topology;

import java.time.Instant;
import java.util.List;

public record NationalCommunicationInstitutionalJudiciaryPopulationSizingResponse(
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
}
