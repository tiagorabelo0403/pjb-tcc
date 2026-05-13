package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbSurfaceAuthPainelPublicDisciplineTest {

    @Test
    void shouldFlagTargetControllersAsCleanFromWildcardAndNestedServiceExposure() {
        var service = new PjbApiSurfaceSanityApplicationService(Path.of("."));
        var aggregate = service.scan();
        String joined = aggregate.issues().stream()
                .map(issue -> issue.location() + "::" + issue.code())
                .reduce("", (left, right) -> left + "\n" + right);
        assertFalse(joined.contains("controller/auth/PasskeyAuthController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/auth/PasskeyStepUpController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/publico/PublicPlenarioGovernanceController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/conciliacao/ConciliadorMediadorPainelController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/perito/PeritoPainelController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/defensor/DefensorPublicoPainelController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/mp/MinisterioPublicoPainelController.java::controller.wildcard.response"));
        assertTrue(aggregate.controllersInspecionados() > 0);
    }
}
