package com.tcc.pjb.backend.service.professional;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalOrganExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalOrganUnitView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendVisualThemeView;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProfessionalOrganExecutiveDashboardStructureTest {

    @Test
    void shouldKeepInstitutionalOrganSurfaceStable() {
        PjbFrontendProfessionalOrganExecutiveDashboardView view = new PjbFrontendProfessionalOrganExecutiveDashboardView(
                Instant.now(),
                "MAGISTRATURA",
                "MAGISTRATURE_ORGAN_EXECUTIVE",
                "Painel institucional do gabinete e colegiado",
                "Gabinete 03 · CE · FORTALEZA",
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
                List.of(new PjbFrontendProfessionalOrganUnitView(
                        "GAB-03",
                        "Gabinete 03",
                        "CE · FORTALEZA · 12 processos · 8 grants",
                        "GAB-03",
                        12,
                        8,
                        2,
                        "#002776",
                        "#12315E",
                        "/api/v1/professional/access-grants/governance-dashboard"
                )),
                List.of("PROFESSIONAL_ORGAN_EXECUTIVE_2026"),
                List.of("/api/v1/frontend/app/professional/workspace/magistrature-organ-dashboard"),
                List.of()
        );
        assertNotNull(view.visualTheme());
        assertEquals("MAGISTRATURE_ORGAN_EXECUTIVE", view.dashboardKind());
        assertEquals("GAB-03", view.organizationalUnits().get(0).key());
    }
}
