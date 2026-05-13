package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import org.junit.jupiter.api.Test;

class PjbControllerInlineDtoDisciplineTest {

    @Test
    void naoMarcaControllerSemInlineDtoComoProblema() {
        PjbApiSurfaceSanityApplicationService service = new PjbApiSurfaceSanityApplicationService();
        assertTrue(service.auditar().issues().stream().noneMatch(issue -> issue.code().equals("controller.inline.dto") && issue.location().contains("AdminBackfillController")));
    }
}
