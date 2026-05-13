package com.tcc.pjb.backend.core.comunicacao.institucional.gate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateState;
import com.tcc.pjb.backend.core.comunicacao.institucional.gate.domain.InstitutionalGateStatus;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalCommunicationGateApplicationServiceTest {

    @Test
    void shouldKeepGateBlockedUntilFulfillment() {
        Instant now = Instant.parse("2026-03-20T12:00:00Z");
        InstitutionalGateState gate = new InstitutionalGateState(
                "gate-1",
                "exp-1",
                10L,
                "0001",
                "GATE-MP",
                InstitutionalGateStatus.AGUARDANDO_CIENCIA,
                true,
                "Fluxo sensível",
                null,
                now,
                now,
                null,
                List.of("inicial"),
                "hash-1"
        );
        InstitutionalGateState afterScience = gate.withStatus(
                InstitutionalGateStatus.AGUARDANDO_CUMPRIMENTO,
                "CIENCIA_INSTITUCIONAL",
                now.plusSeconds(30),
                List.of("ciencia"),
                "hash-2"
        );
        InstitutionalGateState released = afterScience.withStatus(
                InstitutionalGateStatus.LIBERADO,
                "CUMPRIMENTO_INSTITUCIONAL",
                now.plusSeconds(60),
                List.of("cumprido"),
                "hash-3"
        );

        assertTrue(afterScience.bloqueado());
        assertEquals(InstitutionalGateStatus.LIBERADO, released.status());
        assertTrue(!released.bloqueado());
    }
}
