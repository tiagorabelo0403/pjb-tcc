package com.tcc.pjb.backend.modules.acordo.application;

import com.tcc.pjb.backend.modules.acordo.domain.AcordoPropostaStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoPropostaTipo;
import java.math.BigDecimal;
import java.time.Instant;

public record AcordoPropostaSnapshot(
        Long id,
        Long sessaoId,
        Long autorId,
        AcordoPropostaTipo tipo,
        BigDecimal valor,
        String termosJson,
        Instant validadeAte,
        AcordoPropostaStatus status,
        boolean criadaPorIa,
        boolean revisadaPorHumano,
        Long revisadaPorId,
        Instant revisadaEm,
        Instant createdAt
) {
    public boolean expiradaEm(Instant now) {
        return validadeAte != null && !validadeAte.isAfter(now);
    }

    public AcordoPropostaSnapshot withRevisaoHumana(Long revisorId, Instant now) {
        return new AcordoPropostaSnapshot(id, sessaoId, autorId, tipo, valor, termosJson, validadeAte,
                AcordoPropostaStatus.PENDENTE, criadaPorIa, true, revisorId, now, createdAt);
    }

    public AcordoPropostaSnapshot withStatus(AcordoPropostaStatus novoStatus) {
        return new AcordoPropostaSnapshot(id, sessaoId, autorId, tipo, valor, termosJson, validadeAte,
                novoStatus, criadaPorIa, revisadaPorHumano, revisadaPorId, revisadaEm, createdAt);
    }
}
