package com.tcc.pjb.backend.core.processo.unificado.domain;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ProcessoMalhaNacionalAggregate(
        Long processoId,
        String numeroProcesso,
        String ramoDireito,
        int totalVerticesIdentidade,
        int totalProcessosCorrelatos,
        long totalDocumentosCriticos,
        long totalBloqueios,
        NivelSigilo nivelSigiloAtual,
        NivelSigilo nivelSigiloRecomendado,
        boolean travaDistribuicaoOuFluxo,
        List<String> hotspots,
        List<ProcessoMalhaNacionalRisco> riscos,
        List<String> fundamentos,
        Instant geradoEm
) {
    public ProcessoMalhaNacionalAggregate {
        numeroProcesso = Objects.toString(numeroProcesso, "").trim();
        ramoDireito = Objects.toString(ramoDireito, "NAO_INFORMADO").trim();
        totalVerticesIdentidade = Math.max(0, totalVerticesIdentidade);
        totalProcessosCorrelatos = Math.max(0, totalProcessosCorrelatos);
        totalDocumentosCriticos = Math.max(0L, totalDocumentosCriticos);
        totalBloqueios = Math.max(0L, totalBloqueios);
        nivelSigiloAtual = nivelSigiloAtual == null ? NivelSigilo.PUBLICO : nivelSigiloAtual;
        nivelSigiloRecomendado = nivelSigiloRecomendado == null ? nivelSigiloAtual : nivelSigiloRecomendado;
        hotspots = hotspots == null ? List.of() : List.copyOf(hotspots);
        riscos = riscos == null ? List.of() : List.copyOf(riscos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        geradoEm = geradoEm == null ? Instant.now() : geradoEm;
    }
}
