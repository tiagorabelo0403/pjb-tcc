package com.tcc.pjb.backend.service.ui.branding;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InstitutionalPanelVisualComposerServiceTest {

    private final InstitutionalPanelVisualComposerService service = new InstitutionalPanelVisualComposerService();

    @Test
    void deveResolverLayoutEstreitoParaInquerito() {
        Map<String, Object> branding = Map.of(
                "displayName", "Polícia Civil",
                "unitDisplayName", "Delegacia Seccional",
                "palette", List.of("#283593", "#F4F6FD", "#132238"),
                "assets", Map.of("banner", Map.of("storageKey", "ui/institutional-branding/policia-civil/banner.webp"))
        );
        Map<String, Object> out = service.compose(new InstitutionalPanelVisualComposerService.ResolveRequest("POLICIA_CIVIL", "PAINEL_INQUERITO_MULTIMIDIA", branding));
        assertEquals("NARROW_EVIDENCE_STREAM", out.get("layoutMode"));
        assertTrue(out.containsKey("institutionStrip"));
        assertTrue(out.containsKey("workspacePage"));
    }
}
