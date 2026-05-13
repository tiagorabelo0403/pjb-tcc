package com.tcc.pjb.backend.model.dto.cidadao;

import java.time.LocalDateTime;

public record CidadaoComunicacaoJudicialDto(
        String expedicaoUuid,
        Long processoId,
        String numeroUnificado,
        String titulo,
        String resumo,
        String tipoComunicacao,
        String status,
        String modalidade,
        boolean requerCiencia,
        boolean prazoAtivo,
        LocalDateTime expedidaEm,
        LocalDateTime prazoInicioEm,
        LocalDateTime presuncaoEntregaEm,
        Links links
) {
}
