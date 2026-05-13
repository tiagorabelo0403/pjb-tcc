package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbSurfaceDelegadoOficialAdminSecurityDisciplineTest {

    @Test
    void shouldKeepDelegadoOficialAdminControllersCleanFromWildcardRawMapAndNestedServiceContracts() {
        var service = new PjbApiSurfaceSanityApplicationService(Path.of("."));
        var aggregate = service.scan();
        String joined = aggregate.issues().stream()
                .map(issue -> issue.location() + "::" + issue.code())
                .reduce("", (left, right) -> left + "\n" + right);

        assertFalse(joined.contains("controller/JudgeDelegationController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/leilao/LeiloeiroJudicialPainelController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/juiz/JuizGabineteDecisionalController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/oficial_justica/OficialJusticaPainelController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/oficial_justica/OficialJusticaPainelController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/delegado/DelegadoPainelController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/delegado/DelegadoPainelController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/security/PanicController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/security/SecurityChallengeController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/security/WebAuthnController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/security/BodyHashController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/security/AdvogadoBaptismController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/security/RequestHashController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/admin/AdminRitoDiagnosticsController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/admin/AdminRitoDiagnosticsController.java::controller.raw.map.response"));
        assertFalse(joined.contains("controller/admin/NationalObservabilityController.java::controller.wildcard.response"));
        assertFalse(joined.contains("controller/admin/NationalObservabilityController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/admin/AdministradorNacionalGovernanceController.java::controller.raw.map.response"));
        assertFalse(joined.contains("controller/admin/AdministradorNacionalGovernanceController.java::controller.nested.service.contract"));
        assertTrue(aggregate.controllersInspecionados() > 0);
    }
}
