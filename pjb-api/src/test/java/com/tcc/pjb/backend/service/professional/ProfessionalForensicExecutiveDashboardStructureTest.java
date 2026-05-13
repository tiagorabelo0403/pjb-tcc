package com.tcc.pjb.backend.service.professional;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalWorkspaceExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendVisualThemeView;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProfessionalForensicExecutiveDashboardStructureTest {

    @Test
    void shouldKeepBrazilExecutiveThemeKeyStable() {
        PjbFrontendProfessionalWorkspaceExecutiveDashboardView view = new PjbFrontendProfessionalWorkspaceExecutiveDashboardView(
                Instant.now(),
                "MAGISTRATURA",
                "MAGISTRATURA_PANEL",
                "Magistratura · JUIZ",
                "CE / FORTALEZA",
                new PjbFrontendVisualThemeView(
                        "BRAZIL_EXECUTIVE_2026",
                        "Brasil Executivo",
                        "#009C3B",
                        "#FFDF00",
                        "#002776",
                        "#006B2D",
                        "#FFFFFF",
                        "#06162E",
                        "#0C2247",
                        "#12315E",
                        "#009C3B",
                        "#002776",
                        List.of("#009C3B", "#FFDF00", "#002776")
                ),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of("PROFESSIONAL_EXECUTIVE_DASHBOARD"),
                List.of("/api/v1/frontend/app/professional/workspace/executive-dashboard"),
                List.of()
        );
        assertNotNull(view.visualTheme());
        assertEquals("BRAZIL_EXECUTIVE_2026", view.visualTheme().key());
        assertEquals("/api/v1/frontend/app/professional/workspace/executive-dashboard", view.quickRoutes().get(0));
    }
}
