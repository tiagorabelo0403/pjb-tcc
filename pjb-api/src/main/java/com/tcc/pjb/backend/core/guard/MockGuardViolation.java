package com.tcc.pjb.backend.core.guard;

import com.tcc.pjb.backend.core.id.PjbUuidV7Generator;
import java.time.Instant;
import java.util.UUID;

public record MockGuardViolation(
        UUID violationId,
        String servico,
        String propriedade,
        MockGuardProfile perfil,
        String motivo,
        Instant detectadoEm
) {
    public static MockGuardViolation of(String servico, String propriedade, MockGuardProfile perfil) {
        return new MockGuardViolation(
                PjbUuidV7Generator.generate(),
                servico,
                propriedade,
                perfil,
                "mock habilitado em ambiente real: " + propriedade + "=true com profile " + perfil.name(),
                Instant.now()
        );
    }
}
