package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertFalse;
import com.tcc.pjb.backend.core.quality.apisurface.application.PjbApiSurfaceSanityApplicationService;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PjbOperationalControllerSurfaceDisciplineTest {

    @Test
    void naoDeveExporMapCruOuRecordInlineNosControllersOperacionaisAtualizados() {
        PjbApiSurfaceSanityApplicationService service = new PjbApiSurfaceSanityApplicationService(Path.of("."));
        var aggregate = service.auditar();
        Set<String> alvos = Set.of(
                "src/main/java/com/tcc/pjb/backend/controller/JobsController.java",
                "src/main/java/com/tcc/pjb/backend/controller/TimelineController.java",
                "src/main/java/com/tcc/pjb/backend/controller/WorkItemController.java",
                "src/main/java/com/tcc/pjb/backend/controller/distribuicao/DistribuicaoProcessualNacionalController.java",
                "src/main/java/com/tcc/pjb/backend/controller/juiz/JuizGabineteDecisionalController.java",
                "src/main/java/com/tcc/pjb/backend/controller/admin/AdminProceduralGovernanceController.java"
        );
        boolean hasIssue = aggregate.issues().stream()
                .filter(issue -> alvos.contains(issue.alvo()))
                .anyMatch(issue -> issue.codigo().equals("controller.raw.map.response")
                        || issue.codigo().equals("controller.inline.dto")
                        || issue.codigo().equals("service.nested.exposure")
                        || issue.codigo().equals("controller.aggregate.exposure"));
        assertFalse(hasIssue);
    }
}
