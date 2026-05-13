package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbSurfaceDisciplineTest {

    @Test
    void scannersDetectNoNestedServiceContractOrInlineDtoOnCoreSurfaceControllers() {
        PjbApiSurfaceSanityApplicationService service = new PjbApiSurfaceSanityApplicationService(Path.of("."));
        String issueText = service.scan().issues().toString();
        assertFalse(issueText.contains("RadarJurisprudenciaController") && issueText.contains("service.nested.exposure"));
        assertFalse(issueText.contains("TemaRecursoRepetitivoController") && issueText.contains("controller.nested.service.contract"));
        assertFalse(issueText.contains("PeritoOperacionalEnhancedController") && issueText.contains("controller.raw.map.response"));
        assertFalse(issueText.contains("PerfilCapabilityMatrixExtensionController") && issueText.contains("controller.inline.dto"));
    }
}
