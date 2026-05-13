package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbSurfaceProcessualMarketplaceResidualDisciplineTest {

    @Test
    void shouldKeepResidualMarketplaceControllersCleanFromWildcardRawMapAndNestedServiceContracts() {
        var service = new PjbApiSurfaceSanityApplicationService(Path.of("."));
        var aggregate = service.scan();
        String joined = aggregate.issues().stream()
                .map(issue -> issue.location() + "::" + issue.code())
                .reduce("", (left, right) -> left + "\n" + right);

        assertFalse(joined.contains("controller/admin/FederalismoRedistribuicaoController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/admin/AdminJudicialConnectorOpsController.java::controller.raw.map.response"));
        assertFalse(joined.contains("controller/lgpd/LgpdProcessualSensibilityController.java::controller.raw.map.response"));
        assertFalse(joined.contains("controller/processual/routing/ProcessualRoutingController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/processual/prazos/PrazoProcessualNacionalController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/processual/pauta/PautaAudienciaNacionalController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/processual/recursal/admissibilidade/RecursalAdmissibilityController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/processual/integration/intertribunal/LitispendenciaIntertribunalController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/processual/TrabalhistaCalculoController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/processual/calculo/TrabalhistaCalculoLegacyController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/processo/ApiMarketplaceController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/processo/ApiMarketplaceOAuthController.java::controller.raw.map.response"));
        assertFalse(joined.contains("controller/processo/ApiMarketplaceOAuthController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/processo/ApiMarketplaceAdminController.java::controller.nested.service.contract"));
        assertFalse(joined.contains("controller/processo/SigiloZeroKnowledgeProofController.java::controller.nested.service.contract"));
        assertTrue(aggregate.controllersInspecionados() > 0);
    }
}
