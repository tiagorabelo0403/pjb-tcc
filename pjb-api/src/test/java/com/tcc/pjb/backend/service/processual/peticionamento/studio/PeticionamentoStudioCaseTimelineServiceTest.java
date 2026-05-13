package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeticionamentoStudioCaseTimelineServiceTest {

    @Test
    void deveConstruirTimelineComFatosEJanelaRecursal() {
        PeticionamentoStudioCaseTimelineService service = new PeticionamentoStudioCaseTimelineService();

        var report = service.build(new PeticionamentoStudioCaseTimelineService.ResolveRequest(
                "Apelação cível",
                List.of("Em 10/03/2026 foi publicada a sentença de improcedência.", "Em 15/03/2026 houve intimação da parte autora."),
                List.of(Map.of("label", "sentenca.pdf", "summary", "Sentença recorrida e fundamento de improcedência.")),
                "RECURSAL",
                "APELACAO",
                false,
                List.of(),
                false,
                null,
                List.of()
        ));

        assertEquals("PETITION_TIMELINE_RECURSAL_V2", report.workspace().get("profile"));
        assertTrue(report.items().stream().anyMatch(item -> "FATO".equals(item.get("phase"))));
        assertTrue(report.items().stream().anyMatch(item -> "PROCEDURAL".equals(item.get("phase"))));
        assertTrue(report.items().stream().anyMatch(item -> "10/03/2026".equals(item.get("dateHint"))));
    }
}
