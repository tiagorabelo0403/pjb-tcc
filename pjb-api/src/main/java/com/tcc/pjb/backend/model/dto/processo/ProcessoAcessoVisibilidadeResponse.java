package com.tcc.pjb.backend.model.dto.processo;

import java.time.LocalDateTime;
import java.util.List;

public record ProcessoAcessoVisibilidadeResponse(
        LocalDateTime generatedAt,
        Long processoId,
        LocalDateTime ultimaLeituraInstitucionalAt,
        List<ProcessoAcessoCategoriaResumo> categorias,
        List<ProcessoUltimoAcessoPerfilResumo> ultimosAcessosPorPerfil,
        List<ProcessoResponsabilidadeAtualResumo> responsabilidadesAtuais,
        List<ProcessoPapelAssumidoResumo> papeisAtivos,
        List<String> mensagens
) {
}
