package com.tcc.pjb.backend.model.dto.processual.completude;

import com.tcc.pjb.backend.model.dto.processual.completude.apisurface.ProcessoApiSurfaceSanityResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.certificacao.ProcessoCertificacaoOperacionalResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.codebase.ProcessoCodebaseSanityResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.infraestrutura.ProcessoInfraestruturaSoberanaResponse;
import com.tcc.pjb.backend.model.dto.processual.completude.substituicao.ProcessoSubstituicaoLegadosResponse;
import java.time.Instant;
import java.util.List;

public record ProcessoFechamentoTotalResponse(
        Long processoId,
        String numeroProcesso,
        String readiness,
        long scoreGeral,
        ProcessoCompletudeModuloResponse antiOrfao,
        ProcessoCompletudeModuloResponse sinalizacao,
        ProcessoCompletudeModuloResponse plantaoSubstituicao,
        ProcessoCompletudeModuloResponse analyticsNacional,
        ProcessoCompletudeModuloResponse operacaoTransversal,
        ProcessoInfraestruturaSoberanaResponse infraestruturaSoberana,
        ProcessoCertificacaoOperacionalResponse certificacaoOperacional,
        ProcessoSubstituicaoLegadosResponse substituicaoLegados,
        ProcessoCodebaseSanityResponse codebaseSanity,
        ProcessoApiSurfaceSanityResponse apiSurface,
        List<String> alertas,
        List<String> plano,
        Instant geradoEm
) {
    public ProcessoFechamentoTotalResponse {
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso;
        readiness = readiness == null ? "NOT_READY" : readiness;
        alertas = alertas == null ? List.of() : List.copyOf(alertas);
        plano = plano == null ? List.of() : List.copyOf(plano);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
