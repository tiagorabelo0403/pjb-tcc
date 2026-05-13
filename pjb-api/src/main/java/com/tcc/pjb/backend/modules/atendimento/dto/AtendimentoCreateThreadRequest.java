package com.tcc.pjb.backend.modules.atendimento.dto;

public record AtendimentoCreateThreadRequest(
    Long processoId,
    Long advogadoId,
    Long cidadaoUsuarioId,
    String cidadaoCpf
) {
}
