package com.tcc.pjb.backend.modules.acordo.application;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoConfidencialidadeNivel;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoSessaoStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoTipoSala;
import java.time.Instant;

public record AcordoSessaoSnapshot(
        Long id,
        Long processoId,
        AcordoTipoSala tipoSala,
        AcordoSessaoStatus status,
        Long abertaPorId,
        Instant abertaEm,
        Instant expiraEm,
        String motivoAbertura,
        boolean segredoJustica,
        AcordoConfidencialidadeNivel confidencialidadeNivel,
        boolean cejuscReferenciado,
        Instant homologadoEm,
        Long homologadoPorId,
        Instant createdAt
) {
    public boolean expiradaEm(Instant now) {
        return expiraEm != null && !expiraEm.isAfter(now);
    }

    public AcordoSessaoSnapshot withStatus(AcordoSessaoStatus novoStatus) {
        return new AcordoSessaoSnapshot(id, processoId, tipoSala, novoStatus, abertaPorId, abertaEm, expiraEm,
                motivoAbertura, segredoJustica, confidencialidadeNivel, cejuscReferenciado, homologadoEm,
                homologadoPorId, createdAt);
    }

    public AcordoSessaoSnapshot withHomologacao(AcordoSessaoStatus novoStatus, Instant data, Long usuarioId) {
        return new AcordoSessaoSnapshot(id, processoId, tipoSala, novoStatus, abertaPorId, abertaEm, expiraEm,
                motivoAbertura, segredoJustica, confidencialidadeNivel, cejuscReferenciado, data, usuarioId, createdAt);
    }
}
