package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbSurfaceMinisterAdminIntelligenceDisciplineTest {

    @Test
    void round82ControllersDoNotExposeLegacySurfacePatterns() {
        PjbApiSurfaceSanityApplicationService service = new PjbApiSurfaceSanityApplicationService(Path.of(""));
        var aggregate = service.auditar();
        assertTrue(aggregate.issues().stream().noneMatch(issue -> issue.location().endsWith("MinistroPlenarioController.java")
                && (issue.code().equals("controller.raw.map.response")
                || issue.code().equals("controller.nested.service.contract")
                || issue.code().equals("service.nested.exposure"))));
        assertTrue(aggregate.issues().stream().noneMatch(issue -> issue.location().endsWith("MinistroRepercussaoGeralController.java")
                && (issue.code().equals("controller.nested.service.contract")
                || issue.code().equals("service.nested.exposure"))));
        assertTrue(aggregate.issues().stream().noneMatch(issue -> issue.location().endsWith("AdminJudicialConnectorCryptoCommandCenterController.java")
                && issue.code().equals("controller.raw.map.response")));
        assertTrue(aggregate.issues().stream().noneMatch(issue -> issue.location().endsWith("RadarPadroesController.java")
                && issue.code().equals("controller.nested.service.contract")));
        assertTrue(aggregate.issues().stream().noneMatch(issue -> issue.location().endsWith("TetoProcessualController.java")
                && issue.code().equals("controller.entity.exposure")));
    }
}
