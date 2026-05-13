package com.tcc.pjb.backend.core.processo.eca;

import java.time.Duration;

public record EcaAudienciaApresentacaoRule(
        Duration prazoMaximoAposApreensao,
        boolean defensorPublicoObrigatorio,
        boolean mpObrigatorio,
        boolean ouvirAdolescenteSeparadamente,
        boolean sigiloDepoimento,
        String fundamentoConstitucional
) {
    public static EcaAudienciaApresentacaoRule padrao() {
        return new EcaAudienciaApresentacaoRule(
                Duration.ofHours(24),
                true,
                true,
                true,
                true,
                "ECA art. 184 — audiência de apresentação em 24h; CF art. 227 — prioridade absoluta"
        );
    }
}
