package com.tcc.pjb.backend.model.dto.processual.completude.infraestrutura;

import java.time.Instant;
import java.util.List;
import com.tcc.pjb.backend.model.dto.processual.completude.ProcessoCompletudeModuloResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.certificacao.ProcessoCertificacaoOperacionalResponse;

public record ProcessoInfraestruturaSoberanaResponse(
        Long processoId,
        String numeroProcesso,
        ProcessoCompletudeModuloResponse fonte,
        ProcessoCompletudeModuloResponse cumprimento,
        ProcessoCompletudeModuloResponse cooperacao,
        ProcessoCertificacaoOperacionalResponse certificacao,
        ProcessoCompletudeModuloResponse gemeoDigital,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoInfraestruturaSoberanaResponse {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
