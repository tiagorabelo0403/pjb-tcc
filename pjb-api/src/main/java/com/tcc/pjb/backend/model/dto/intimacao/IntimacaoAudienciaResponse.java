package com.tcc.pjb.backend.model.dto.intimacao;

import com.tcc.pjb.backend.model.entity.IntimacaoAudiencia;
import com.tcc.pjb.backend.model.entity.enums.StatusIntimacaoAudiencia;
import com.tcc.pjb.backend.model.entity.enums.TipoDestinatarioIntimacao;

import java.time.Instant;

public record IntimacaoAudienciaResponse(
        Long id,
        Long audienciaId,
        String destinatarioNome,
        TipoDestinatarioIntimacao destinatarioTipo,
        String destinatarioOab,
        String destinatarioEmail,
        String canal,
        StatusIntimacaoAudiencia status,
        Instant prazoCiencia,
        Instant enviadaEm,
        Instant cienciaEm,
        Instant criadoEm
) {
    public static IntimacaoAudienciaResponse de(IntimacaoAudiencia e) {
        return new IntimacaoAudienciaResponse(
                e.getId(),
                e.getAudiencia() != null ? e.getAudiencia().getId() : null,
                e.getDestinatarioNome(), e.getDestinatarioTipo(),
                e.getDestinatarioOab(), e.getDestinatarioEmail(),
                e.getCanal(), e.getStatus(),
                e.getPrazoCiencia(), e.getEnviadaEm(), e.getCienciaEm(), e.getCriadoEm()
        );
    }
}
