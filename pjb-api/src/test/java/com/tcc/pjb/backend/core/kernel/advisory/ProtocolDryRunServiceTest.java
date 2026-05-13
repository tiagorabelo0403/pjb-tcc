package com.tcc.pjb.backend.core.kernel.advisory;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoValidateResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ProtocolDryRunServiceTest {

    private final ProtocolDryRunService service = new ProtocolDryRunService();

    @Test
    void shouldBlockDryRunWhenValidationFails() {
        LaianePeticaoAssistRequest request = LaianePeticaoAssistRequest.builder()
                .kind("PETICAO_INICIAL")
                .classeTpu("7")
                .ramoDireito("CIVIL")
                .build();
        var canonical = new ProceduralCanonicalResolver.CanonicalContext(
                null,
                com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual.COMUM_ORDINARIO,
                "CIVIL",
                "7",
                "Procedimento Comum Cível",
                "ESTADUAL",
                "TJCE",
                "Tribunal de Justiça do Ceará",
                "PJE",
                List.of("AUTOR", "REU"),
                List.of("PETICAO_INICIAL"),
                List.of("ESTADUAL"),
                java.util.Map.of()
        );
        LaianePeticaoValidateResponse validator = LaianePeticaoValidateResponse.builder()
                .ok(false)
                .errors(List.of("erro estrutural"))
                .build();
        LegalCoherenceReport coherence = new LegalCoherenceReport(0.20d, true, List.of(), List.of(), List.of("corrigir"));

        ProtocolDryRunReport report = service.simulateRequest(request, canonical, "COMUM_ORDINARIO", validator, null, null, null, coherence, 0.25d);

        assertNotNull(report);
        assertFalse(report.apto());
        assertEquals("BLOCKED_IN_DRY_RUN", report.status());
        assertTrue(report.checks().stream().anyMatch(c -> "PETITION_VALIDATION".equals(c.code()) && !c.passed()));
    }
}
