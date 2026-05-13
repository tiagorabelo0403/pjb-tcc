package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbSurfaceAiAdminDisciplineTest {

    @Test
    void shouldKeepAiAndConnectorHubControllersAlignedWithExplicitSurfaceContracts() {
        var service = new PjbApiSurfaceSanityApplicationService(Path.of("."));
        var aggregate = service.scan();
        String joined = aggregate.issues().stream()
                .map(issue -> issue.location() + "::" + issue.code())
                .reduce("", (left, right) -> left + "\n" + right);

        assertFalse(joined.contains("ai/juridica/api/LegalAiController.java::controller.wildcard.response"));
        assertFalse(joined.contains("ai/juridica/api/LegalAiController.java::controller.inline.dto"));
        assertFalse(joined.contains("ai/juridica/api/AjuizamentoIntentController.java::controller.raw.map.response"));
        assertFalse(joined.contains("judicial/connectors/api/admin/AdminJudicialConnectorHubController.java::controller.raw.map.response"));
        assertTrue(aggregate.controllersInspecionados() > 0);
    }
}
