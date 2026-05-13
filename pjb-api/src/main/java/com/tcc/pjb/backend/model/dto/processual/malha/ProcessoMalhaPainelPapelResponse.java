package com.tcc.pjb.backend.model.dto.processual.malha;

import java.time.Instant;
import java.util.List;

public record ProcessoMalhaPainelPapelResponse(
        Long processoId,
        String numeroProcesso,
        String papel,
        String ramo,
        String statusGeral,
        ProcessoMalhaAtorResponse ator,
        ProcessoMalhaSigiloResponse sigilo,
        List<ProcessoMalhaWidgetResponse> widgets,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoMalhaPainelPapelResponse {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso.trim();
        papel = papel == null ? "CIDADAO" : papel.trim();
        ramo = ramo == null ? "NAO_INFORMADO" : ramo.trim();
        statusGeral = statusGeral == null ? "ESTAVEL" : statusGeral.trim();
        widgets = widgets == null ? List.of() : List.copyOf(widgets);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
