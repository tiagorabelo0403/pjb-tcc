package com.tcc.pjb.backend.service.professional;

import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalRoleExecutiveDashboardView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendProfessionalRoleSegmentView;
import com.tcc.pjb.backend.core.frontend.app.domain.PjbFrontendVisualThemeView;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ProfessionalRoleExecutiveDashboardStructureTest {

    @Test
    void shouldKeepMagistratureRoleSurfaceStable() {
        PjbFrontendProfessionalRoleExecutiveDashboardView view = new PjbFrontendProfessionalRoleExecutiveDashboardView(
                Instant.now(),
                "MAGISTRATURA",
                "MAGISTRATURE_ROLE_EXECUTIVE",
                "Painel Executivo da Magistratura",
                "TJCE · CE · FORTALEZA",
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
                List.of(new PjbFrontendProfessionalRoleSegmentView("MAG_JURISDICTION", "Competência jurisdicional", "relatoria, colegiado e gabinete", "#002776", List.of(), List.of(), List.of("/api/v1/frontend/app/professional/workspace/magistrature-executive-dashboard"))),
                List.of(),
                List.of(),
                List.of(),
                List.of("MAGISTRATURE_EXECUTIVE_DASHBOARD_2026"),
                List.of("/api/v1/frontend/app/professional/workspace/magistrature-executive-dashboard"),
                List.of()
        );
        assertNotNull(view.visualTheme());
        assertEquals("MAGISTRATURE_ROLE_EXECUTIVE", view.dashboardKind());
        assertEquals("MAG_JURISDICTION", view.segments().get(0).key());
    }
}
