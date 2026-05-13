package com.tcc.pjb.backend.model.dto.organizacao;

import java.util.List;

public record GrupoProcessualDto(
        String chave,
        String descricao,
        long total,
        long ativos,
        long comAudienciaHoje,
        long comPrazoVencendo,
        List<ProcessoResumoDto> processos
) {
}
