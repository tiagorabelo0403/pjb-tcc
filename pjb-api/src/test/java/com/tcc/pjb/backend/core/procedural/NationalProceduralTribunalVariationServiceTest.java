package com.tcc.pjb.backend.core.procedural;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.routing.TribunalProtocolRoutingService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalProceduralTribunalVariationServiceTest {

    @Test
    void detailResolvesSystemAndOperationalRules() {
        TribunalProtocolRoutingService routingService = mock(TribunalProtocolRoutingService.class);
        when(routingService.resolve(Map.of(
                "tribunalCodigo", "TJRS",
                "varaPretendida", "VARA_COMPETENTE",
                "tipoJustica", "ESTADUAL",
                "rito", "COMUM_ORDINARIO",
                "ramoDireito", "CIVIL"
        ), "COMUM_ORDINARIO", "CIVIL", "ESTADUAL", false)).thenReturn(
                new TribunalProtocolRoutingService.RoutingDecision(
                        "TJRS",
                        "Tribunal de Justiça do Rio Grande do Sul",
                        JudicialSystem.EPROC,
                        new JudicialSubmissionCapability(JudicialSystem.EPROC, true, true, true, false, false, false, false, true, List.of("PDF"), List.of(), List.of(), "https://eproc.example"),
                        "COMPETENCIA_ESTADUAL",
                        false,
                        false,
                        List.of(),
                        Map.of(),
                        Instant.now()
                )
        );

        NationalProceduralTribunalVariationService service = new NationalProceduralTribunalVariationService(routingService);

        NationalProceduralTribunalVariationRow row = service.describe("TJRS", null, "COMUM_ORDINARIO", "ESTADUAL");

        assertEquals("TJRS", row.tribunalCodigo());
        assertEquals("EPROC", row.judicialSystem());
        assertFalse(row.localRules().isEmpty());
        assertTrue(row.protocolChannels().contains("EPROC"));
    }
}
