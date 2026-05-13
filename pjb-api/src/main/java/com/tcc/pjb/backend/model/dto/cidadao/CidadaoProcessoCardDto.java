package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;
import java.util.List;

public record CidadaoProcessoCardDto(
        Long processoId,
        String numeroUnificado,
        String classeProcessual,
        String assunto,

        String ritoCode,
        String ritoTitle,
        String ramoSugerido,
        Double ritoConfidence,
                        Boolean ritoNeedsReview,

                List<String> ritoReasons,

        String status,
        String faseAtual,
        String nivelSigilo,
        LocalDateTime dataUltimaMovimentacao,

        List<String> uiTokens,

        String ultimaMovimentacaoResumo,
        LocalDateTime ultimaMovimentacaoData,

        PrazoInfoDto prazo,

        LocalDateTime proximaAudienciaDataHora,
        String proximaAudienciaTipo,
        String proximaAudienciaModalidade,
        String proximaAudienciaLocal,

        LocalDateTime proximoJulgamentoDataHora,
        String proximoJulgamentoResumo,

        long documentosCount,

        Links links
) {}
